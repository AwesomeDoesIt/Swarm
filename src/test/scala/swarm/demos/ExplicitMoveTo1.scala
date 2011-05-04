package swarm.demos

import swarm.transport.{Transporter, InetTransporter, InetLocation}
import swarm.{Bee, NoBee, Swarm}
import swarm.Swarm.swarm

object ExplicitMoveTo1 {
  def main(args: Array[String]) = {

    implicit val tx: Transporter = InetTransporter

    InetTransporter.listen(java.lang.Short.parseShort(args(0)));

    if (args.length > 1 && args(1) == "start") {
      Swarm.spawn(emt1Thread);
    }

    while (true) {
      Thread.sleep(1000);
    }
  }

  def emt1Thread(u: Unit): Bee@swarm = {
    val name = scala.Console.readLine("What is your name? : ");
    Swarm.moveTo(new InetLocation(java.net.InetAddress.getLocalHost, 9997))
    val age = Integer.parseInt(readLine("Hello " + name + ", what age are you? : "))
    Swarm.moveTo(new InetLocation(java.net.InetAddress.getLocalHost, 9998))
    println("Wow " + name + ", you're half way to " + (age * 2) + " years old")
    NoBee()
  }
}