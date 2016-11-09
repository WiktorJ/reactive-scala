package actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import auctions.AuctionActorBasic
import common._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by wiktor on 28/10/16.
  */
class Seller(val sellerId: String, val auctionFactory: IAuctionFactory, val names: Array[String], val schedulingInterval: () => Long) extends Actor {


  val searchActor = context.actorSelection("../" + CommonNames.auctionSearchActorName)
  override def receive: Receive = {
    case Start => context.system.scheduler.scheduleOnce(FiniteDuration(schedulingInterval(), TimeUnit.MILLISECONDS)) {
      val auctionId = AuctionsIdGenerator.getNext()
      val auctionName: String = names(Random.nextInt(names.length)) + " " + auctionId
      val actor = auctionFactory.produce(context.system, randBigDecimal(20, 100), self, auctionName)
      searchActor ! AddAuction(auctionName, actor)
      actor ! Start
      self ! Start
    }
    case AuctionSold(_, price, auctionId) =>
      searchActor ! RemoveAuction(auctionId)
      println("Seller " + sellerId + " sold item: " + auctionId + " for " + price)
    case AuctionNotSold(auctionId) =>
      searchActor ! RemoveAuction(auctionId)
      println("Seller " + sellerId + " Did not sell " + auctionId)
  }

  private def randBigDecimal(from: Int, to: Int): BigDecimal = {
    (Math.random() * (to - from)) + from
  }
}


object AuctionsIdGenerator {
  var seq = 0

  def getNext(): Int = {
    seq += 1
    seq
  }
}
