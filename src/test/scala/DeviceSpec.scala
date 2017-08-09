import org.scalatest._
import akka.actor._
import akka.testkit._

import scala.concurrent.duration._
import learn.actors.DeviceManager
import learn.actors.Device

class DeviceSpec extends FunSuite {

  implicit val system = ActorSystem("test-system")

  test("reply with empty reading if no temperature is known") {
    val probe = TestProbe()

    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(Device.RecordTemperature(requestId = 1, 24.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))

    deviceActor.tell(Device.ReadTemperature(requestId = 2), probe.ref)

    val response1 = probe.expectMsgType[Device.RespondTemperature]

    assert(response1.requestId === 2)
    assert(response1.value === Some(24.0))
  }

  test("reply to registration requests") {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(DeviceManager.RequestTrackDevice("group", "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    assert(probe.lastSender === deviceActor)
  }

  test("ignore wrong registration requests") {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(DeviceManager.RequestTrackDevice("wrongGroup", "device"), probe.ref)
    probe.expectNoMsg(Duration(500, MILLISECONDS))

    deviceActor.tell(DeviceManager.RequestTrackDevice("group", "Wrongdevice"), probe.ref)
    probe.expectNoMsg(Duration(500, MILLISECONDS))
  }
}
