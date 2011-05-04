package swarm.demos

import swarm.transport.{Transporter, InetTransporter}

object Listen {
  def main(args: Array[String]) = {
    implicit val tx: Transporter = InetTransporter
    InetTransporter.listen(java.lang.Short.parseShort(args(0)))
    while (true) {
      Thread.sleep(1000)
    }
  }
}