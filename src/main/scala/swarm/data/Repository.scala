package swarm.data

trait Repository {
  def get[A](uid: Long): Option[A]

  def add[A](value: A): Long

  def remove(uid: Long)

  def exists(uid: Long): Boolean
}

object SimpleRepository extends Repository {
  private[this] var nextUid: Long = 0L
  private[this] val store = new collection.mutable.HashMap[Long, Any]()

  def get[A](uid: Long): Option[A] = store.get(uid).asInstanceOf[Option[A]]

  def add[A](value: A): Long = {
    nextUid += 1
    store(nextUid) = value
    nextUid
  }

  def update[A](uid: Long, newValue: A) = store.put(uid, newValue)

  def remove(uid: Long) = store.remove(uid)

  def exists(uid: Long): Boolean = store.contains(uid)
}