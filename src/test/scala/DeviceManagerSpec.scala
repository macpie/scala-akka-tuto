import org.scalatest._
import akka.actor._
import akka.testkit._
import learn.actors._

class DeviceManagerSpec extends FunSuite {

  implicit val system = ActorSystem("test-system")

  test("be able to register a device actor") {
    val probe = TestProbe()
    val managerActor = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    managerActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor2 = probe.lastSender

    assert(deviceActor1 !== deviceActor2)

    // Check that the device actors are working
    deviceActor1.tell(Device.RecordTemperature(requestId = 0, 1.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 0))
    deviceActor2.tell(Device.RecordTemperature(requestId = 1, 2.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))
  }

  test("return same actor for same deviceId") {
    val probe = TestProbe()
    val managerActor = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    managerActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor2 = probe.lastSender

    assert(deviceActor1 === deviceActor2)
  }
}
