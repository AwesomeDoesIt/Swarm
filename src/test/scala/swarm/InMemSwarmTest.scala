package swarm

import org.scalatest.FunSuite
class InMemSwarmTest extends FunSuite {

  test("execution moves from one swarm to another") {
    import Swarm.swarm

    InMemSwarm.getSwarm(InMemLocation(1)).spawn(imst)

    def imst(u: Unit): Bee@swarm = {
      assert(InMemSwarm.currentLocation == null)

      InMemSwarm.getSwarm(InMemLocation(1)).moveTo(InMemLocation(2))
      assert(InMemSwarm.currentLocation == InMemLocation(2))

      InMemSwarm.getSwarm(InMemLocation(2)).moveTo(InMemLocation(1))
      assert(InMemSwarm.currentLocation == InMemLocation(1))

      NoBee()
    }
  }
}

object InMemSwarm {
  val swarm1: InMemSwarm = new InMemSwarm(InMemLocation(1))
  val swarm2: InMemSwarm = new InMemSwarm(InMemLocation(2))

  def getSwarm(location: Location): InMemSwarm = location match {
    case InMemLocation(1) => swarm1
    case InMemLocation(2) => swarm2
  }

  var currentLocation: InMemLocation = _
}

case class InMemLocation(val id: Int) extends Location

class InMemSwarm(val local: InMemLocation) extends SwarmExecutor {
  override def transmit(f: Unit => Bee, destination: Location) {
    InMemSwarm.getSwarm(destination).receive(f)
  }

  def receive(f: Unit => Bee) {
    InMemSwarm.currentLocation = local
    continue(f)
  }
}