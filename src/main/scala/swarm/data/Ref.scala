package swarm.data

import swarm.Swarm
import swarm.Swarm.swarm
import swarm.transport.Location

/**
Represents a reference to an object which may reside on a remote computer.
 If apply() is called to retrieve the remote object, it will result in
 the thread being serialized and moved to the remote computer, before
 returning the object.
 **/
class Ref[A](val typeClass: Class[A], val initLoc: Location, val initUid: Long) extends Serializable {

  private[this] var _location = initLoc
  private[this] var _uid = initUid

  def location: Location = _location

  def uid: Long = _uid

  def relocate(newUid: Long, newLocation: Location) {
    _uid = newUid
    _location = newLocation
  }

  def apply(): A@swarm = {
    Swarm.dereference(this)
    Store(typeClass, uid).getOrElse(throw new RuntimeException("Unable to find item with uid " + uid + " in local store"))
  }

  def update(newValue: A): Unit@swarm = {
    Swarm.dereference(this)
    Store.update(uid, newValue)
  }
}

object Ref {

  import swarm.Swarm.swarm

  def apply[A](location: Location, value: A)(implicit m: scala.reflect.Manifest[A]): Ref[A]@swarm = {
    Swarm.moveTo(location)
    val uid = Store.save(value)
    new Ref[A](m.erasure.asInstanceOf[Class[A]], location, uid);
  }

  def unapply[A](ref: Ref[A]) = {
    Some(ref())
  }
}

class RefMap[A](typeClass: Class[A]) {

  val map = new collection.mutable.HashMap[String, Ref[A]]()

  def get(key: String): Option[A]@swarm = {
    if (map.contains(key)) {
      Some(map(key)())
    } else {
      None
    }
  }

  def put(location: Location, key: String, value: A)(implicit m: scala.reflect.Manifest[A]): Unit@swarm = {
    if (map.contains(key)) {
      // The mapStore knows about this id, so assume that all nodes have a reference to this value in their stores
      val ref: Ref[A] = map(key)
      ref.update(value)
    } else {
      // The mapStore does not know about this id, so assume that no nodes have a reference to this value in their stores; create a Ref and add it to every Swarm store
      val ref: Ref[A] = Ref(location, value)
      map(key) = ref
      // TODO for each location in the cluster, add the ref to the local map
    }
  }
}

object RefMap {

  val map = new collection.mutable.HashMap[String, RefMap[_]]()

  private[this] var _locations: Option[List[Location]] = None

  def locations(locations: List[Location]) {
    _locations = Some(locations)
  }

  def apply[A](typeClass: Class[A], key: String) = {
    val refMap: RefMap[A] = new RefMap(typeClass)
    // TODO for each location in the cluster, add the refMap to the local map
    // the following block is very continuation unfriendly
    /*_locations.map {
      locs => locs.map {
        location =>
          Swarm.moveTo(location)
          map.put(key, refMap)
      }
    }    */
    refMap
  }
}