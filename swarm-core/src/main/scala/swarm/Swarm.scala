package swarm

import data.{Store, Ref}
import transport._
import util.continuations._

/**
 * Swarm owns all of the continuations code. It relies on an implicit
 * SwarmTransporter, which defines how continuations are transported
 * between nodes.
 */
object Swarm {

  type swarm = cpsParam[Bee, Bee]

  /**
   * Called from concrete implementations to run the continuation
   */
  def continue(f: Unit => Bee)(implicit tx: Transporter) {
    execute(reset(f()))
  }

  /**
   * Start a new Swarm task (will return immediately as task is started in a
   * new thread)
   */
  def spawn(f: => Any@swarm)(implicit tx: Transporter) {
    val thread = new Thread() {
      override def run() = execute(reset {
        f
        NoBee()
      })
    }
    thread.start()
  }

  def spawnAndReturn[A](f: => A@swarm)(implicit tx: Transporter, local: Location) = {
    val uuid = java.util.UUID.randomUUID.toString
    val future: Future = new Future
    futures(uuid) = future

    Swarm.spawn {
      val x = f
      Swarm.moveTo(local)
      Swarm.futureResult(uuid, x)
      NoBee()
    }(tx)
    future.get.asInstanceOf[A]
  }


  /**
   * Relocates the code to the given destination
   */
  def moveTo(destination: Location) = shift {
    c: (Unit => Bee) =>
      IsBee(c, destination)
  }

  def relocate[A](ref: Ref[A], destination: Location): Ref[A]@swarm = {
    val refValue = ref()

    moveTo(destination)
    val newRef = new Ref[A](ref.typeClass, destination, Store.save(refValue))

    moveTo(ref.location)
    Store.relocate(ref.uid, newRef)
    ref.relocate(newRef.uid, newRef.location)

    newRef
  }

  def dereference(ref: Ref[_]) = shift {
    c: (Unit => Bee) =>
      RefBee(c, ref)
  }

  /**
   * Executes the continuation if it should be run locally, otherwise
   * relocates to the given destination
   */
  def execute(bee: Bee)(implicit tx: Transporter) {
    bee match {
      case RefBee(f, ref) if (tx.isLocal(ref.location)) =>
        if (!Store.exists(ref.uid)) {
          val newRef = Store.relocated(ref.uid)
          ref.relocate(newRef.uid, newRef.location)
          tx.transport(f, ref.location)
        } else {
          Swarm.continue(f)
        }
      case RefBee(f, ref) => tx.transport(f, ref.location)
      case IsBee(f, destination) if (tx.isLocal(destination)) => Swarm.continue(f)
      case IsBee(f, destination) => tx.transport(f, destination)
      case NoBee() =>
    }
  }

  private class Future(private var _value: Any) {

    def value = _value

    def value_=(value: Any) {
      synchronized {
        _value = value
        notify
      }
    }

    def get: Any = {
      synchronized {
        wait
        value
      }
    }
  }

  private[this] val futures = new collection.mutable.HashMap[String, Future]()

  def futureResult(uuid: String, value: Any) {
    futures.get(uuid).map {
      futureValue =>
        futureValue.value = value
    }
  }
}