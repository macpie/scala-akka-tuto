package learn.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import scala.concurrent.duration._

object DeviceGroup {
  def props(groupId: String): Props = Props(new DeviceGroup(groupId))

  final case class RequestDeviceList(requestId: Long)

  final case class ReplyDeviceList(requestId: Long, ids: Set[String])

  final case class RequestAllTemperatures(requestId: Long)

  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  sealed trait TemperatureReading

  final case class Temperature(value: Double) extends TemperatureReading

  case object TemperatureNotAvailable extends TemperatureReading

  case object DeviceNotAvailable extends TemperatureReading

  case object DeviceTimedOut extends TemperatureReading

}

class DeviceGroup(groupId: String) extends Actor with ActorLogging {

  import learn.actors.DeviceManager.RequestTrackDevice
  import learn.actors.DeviceGroup._

  var deviceIdToActorMap = Map.empty[String, ActorRef]
  var actorToDeviceIdMap = Map.empty[ActorRef, String]
  var nextCollectionId = 0L

  override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)

  override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)

  override def receive: Receive = {
    case trackMsg@RequestTrackDevice(`groupId`, _) =>

      deviceIdToActorMap.get(trackMsg.deviceId) match {
        case Some(deviceActor) =>
          deviceActor.forward(trackMsg)

        case None =>
          log.info("Creating device actor for {}", trackMsg.deviceId)
          val deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId), s"device-${trackMsg.deviceId}")

          context.watch(deviceActor)
          actorToDeviceIdMap += deviceActor -> trackMsg.deviceId
          deviceIdToActorMap += trackMsg.deviceId -> deviceActor
          deviceActor.forward(trackMsg)
      }

    case RequestTrackDevice(groupId, _) =>
      log.warning(
        "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
        groupId, this.groupId
      )

    case RequestDeviceList(requestId) =>
      sender() ! ReplyDeviceList(requestId, deviceIdToActorMap.keySet)

    case Terminated(deviceActor) =>
      val deviceId = actorToDeviceIdMap(deviceActor)
      log.info("Device actor for {} has been terminated", deviceId)
      actorToDeviceIdMap -= deviceActor
      deviceIdToActorMap -= deviceId

    case RequestAllTemperatures(requestId) =>
      context.actorOf(DeviceGroupQuery.props(
        actorToDeviceId = actorToDeviceIdMap,
        requestId = requestId,
        requester = sender(),
        3.seconds
      ))
  }
}