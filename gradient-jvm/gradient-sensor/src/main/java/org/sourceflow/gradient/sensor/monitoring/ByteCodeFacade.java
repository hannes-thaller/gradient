package org.sourceflow.gradient.sensor.monitoring;

import org.sourceflow.gradient.common.CommonEntitySerde;
import org.sourceflow.gradient.common.entities.CommonEntities;
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities;
import org.sourceflow.gradient.sensor.DIContainer;
import org.sourceflow.gradient.sensor.persistence.MonitoringDao;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Static byte code facade to interface generated code with backend.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ByteCodeFacade {
    private static class Frame {
        int elementId;
        long frameId;

        public Frame(int elementId, long frameId) {
            this.elementId = elementId;
            this.frameId = frameId;
        }
    }

    private static final MonitoringDao dao;
    private static final ThreadLocal<Stack<Frame>> elementStack = ThreadLocal.withInitial(Stack::new);
    private static final AtomicLong frameGenerator = new AtomicLong(0L);

    static {
        dao = DIContainer.INSTANCE.getMonitoringDao();
    }

    private static MonitoringEntities.MonitoringEvent createEvent(MonitoringEntities.MonitoringEventType type, long frame, int source, int target) {
        return createEvent(type, frame, source, target, null);
    }

    private static MonitoringEntities.MonitoringEvent createEvent(MonitoringEntities.MonitoringEventType type, long frame, int source, int target, CommonEntities.Datum datum) {
        MonitoringEntities.MonitoringEvent.Builder builder = MonitoringEntities.MonitoringEvent.newBuilder()
                .setType(type)
                .setFrameId(frame)
                .setSource(source)
                .setTarget(target);
        if (datum != null) {
            builder.setDatum(datum);
        }
        return builder.build();
    }

    public static long frame(int source) {
        long frameId = frameGenerator.getAndIncrement();

        final Stack<Frame> stack = elementStack.get();

        MonitoringEntities.MonitoringEvent msg;
        if (stack.isEmpty()) {
            msg = createEvent(MonitoringEntities.MonitoringEventType.FRAME, frameId, 0, source);
        } else {
            final Frame lastFrame = stack.peek();
            msg = createEvent(
                    MonitoringEntities.MonitoringEventType.FRAME, frameId, lastFrame.elementId, source,
                    CommonEntitySerde.INSTANCE.fromLong(lastFrame.frameId)
            );
        }
        stack.push(new Frame(source, frameId));
        dao.reportEvent(msg);

        return frameId;
    }

    public static boolean read(boolean value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.READ, frame, source, target,
                CommonEntitySerde.INSTANCE.fromBoolean(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static char read(char value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.READ, frame, source, target,
                CommonEntitySerde.INSTANCE.fromString(Character.toString(value))
        );
        dao.reportEvent(msg);
        return value;
    }

    public static byte read(byte value, int source, int target, long frame) {
        read(Byte.toUnsignedInt(value), source, target, frame);
        return value;
    }

    public static short read(short value, int source, int target, long frame) {
        read(Short.toUnsignedInt(value), source, target, frame);
        return value;

    }

    public static int read(int value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.READ, frame, source, target,
                CommonEntitySerde.INSTANCE.fromInt(value)
        );
        dao.reportEvent(msg);
        return value;

    }

    public static long read(long value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.READ, frame, source, target,
                CommonEntitySerde.INSTANCE.fromLong(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static float read(float value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.READ, frame, source, target,
                CommonEntitySerde.INSTANCE.fromFloat(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static double read(double value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.READ, frame, source, target,
                CommonEntitySerde.INSTANCE.fromDouble(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static Object read(Object value, int source, int target, long frame) {
        CommonEntities.Datum datum = CommonEntitySerde.INSTANCE.fromReference(value);
        if (datum != null) {
            final MonitoringEntities.MonitoringEvent msg = createEvent(MonitoringEntities.MonitoringEventType.READ, frame, source, target, datum);
            dao.reportEvent(msg);
        }
        return value;
    }

    public static boolean write(boolean value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.WRITE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromBoolean(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static char write(char value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.WRITE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromReference(Character.toString(value))
        );
        dao.reportEvent(msg);
        return value;
    }

    public static byte write(byte value, int source, int target, long frame) {
        write(Byte.toUnsignedInt(value), source, target, frame);
        return value;
    }

    public static short write(short value, int source, int target, long frame) {
        write(Short.toUnsignedInt(value), source, target, frame);
        return value;
    }

    public static int write(int value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.WRITE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromInt(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static long write(long value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.WRITE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromLong(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static float write(float value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.WRITE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromFloat(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static double write(double value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.WRITE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromDouble(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static Object write(Object value, int source, int target, long frame) {
        CommonEntities.Datum datum = CommonEntitySerde.INSTANCE.fromReference(value);
        if (datum != null) {
            final MonitoringEntities.MonitoringEvent msg = createEvent(MonitoringEntities.MonitoringEventType.WRITE, frame, source, target, datum);
            dao.reportEvent(msg);
        }
        return value;
    }

    public static boolean receive(boolean value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromBoolean(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static char receive(char value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromReference(Character.toString(value))
        );
        dao.reportEvent(msg);
        return value;
    }

    public static byte receive(byte value, int source, int target, long frame) {
        receive(Byte.toUnsignedInt(value), source, target, frame);
        return value;
    }

    public static short receive(short value, int source, int target, long frame) {
        receive(Short.toUnsignedInt(value), source, target, frame);
        return value;
    }

    public static int receive(int value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromInt(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static long receive(long value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromLong(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static float receive(float value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromFloat(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static double receive(double value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromDouble(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static Object receive(Object value, int source, int target, long frame) {
        CommonEntities.Datum datum = CommonEntitySerde.INSTANCE.fromReference(value);
        if (datum != null) {
            final MonitoringEntities.MonitoringEvent msg = createEvent(
                    MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target, datum
            );
            dao.reportEvent(msg);
        }
        return value;
    }

    public static void receiveV(boolean value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromBoolean(value)
        );
        dao.reportEvent(msg);
    }

    public static void receiveV(char value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromReference(Character.toString(value))
        );
        dao.reportEvent(msg);
    }

    public static void receiveV(byte value, int source, int target, long frame) {
        receive(Byte.toUnsignedInt(value), source, target, frame);
    }

    public static void receiveV(short value, int source, int target, long frame) {
        receive(Short.toUnsignedInt(value), source, target, frame);
    }

    public static void receiveV(int value, int source, int target, long frame) {

        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromInt(value)
        );
        dao.reportEvent(msg);
    }

    public static void receiveV(long value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromLong(value)
        );
        dao.reportEvent(msg);
    }

    public static void receiveV(float value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromFloat(value)
        );
        dao.reportEvent(msg);
    }

    public static void receiveV(double value, int source, int target, long frame) {
        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target,
                CommonEntitySerde.INSTANCE.fromDouble(value)
        );
        dao.reportEvent(msg);
    }

    public static void receiveV(Object value, int source, int target, long frame) {
        CommonEntities.Datum datum = CommonEntitySerde.INSTANCE.fromReference(value);
        if (datum != null) {
            final MonitoringEntities.MonitoringEvent msg = createEvent(
                    MonitoringEntities.MonitoringEventType.RECEIVE, frame, source, target, datum
            );
            dao.reportEvent(msg);
        }
    }

    public static boolean returns(boolean value, int source, long frame) {
        Frame thisFrame = elementStack.get().pop();
        assert thisFrame.frameId == frame;

        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RETURN, frame, source, 0, CommonEntitySerde.INSTANCE.fromBoolean(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static char returns(char value, int source, long frame) {
        returns(value, source, 0, frame);
        return value;
    }

    public static byte returns(byte value, int source, long frame) {
        returns(value, source, 0, frame);
        return value;
    }

    public static short returns(short value, int source, long frame) {
        returns(value, source, 0, frame);
        return value;
    }

    public static int returns(int value, int source, long frame) {
        returns(value, source, 0, frame);
        return value;
    }

    public static long returns(long value, int source, long frame) {
        returns(value, source, 0, frame);
        return value;
    }

    public static float returns(float value, int source, long frame) {
        returns(value, source, 0, frame);
        return value;
    }

    public static double returns(double value, int source, long frame) {
        returns(value, source, 0, frame);
        return value;
    }

    public static Object returns(Object value, int source, long frame) {
        returns(value, source, 0, frame);
        return value;
    }

    public static void returnsV(int source, long frame) {
        returnsV(source, 0, frame);
    }

    public static boolean returns(boolean value, int source, int target, long frame) {
        if (target == 0) {
            Frame thisFrame = elementStack.get().pop();
            assert thisFrame.frameId == frame;
        }

        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RETURN, frame, source, target,
                CommonEntitySerde.INSTANCE.fromBoolean(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static char returns(char value, int source, int target, long frame) {
        if (target == 0) {
            Frame thisFrame = elementStack.get().pop();
            assert thisFrame.frameId == frame;
        }

        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RETURN, frame, source, target,
                CommonEntitySerde.INSTANCE.fromReference(Character.toString(value))
        );
        dao.reportEvent(msg);
        return value;
    }

    public static byte returns(byte value, int source, int target, long frame) {
        returns(Byte.toUnsignedInt(value), source, target, frame);
        return value;
    }

    public static short returns(short value, int source, int target, long frame) {
        returns(Short.toUnsignedInt(value), source, target, frame);
        return value;
    }

    public static int returns(int value, int source, int target, long frame) {
        if (target == 0) {
            Frame thisFrame = elementStack.get().pop();
            assert thisFrame.frameId == frame;
        }

        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RETURN, frame, source, target,
                CommonEntitySerde.INSTANCE.fromInt(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static long returns(long value, int source, int target, long frame) {
        if (target == 0) {
            Frame thisFrame = elementStack.get().pop();
            assert thisFrame.frameId == frame;
        }

        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RETURN, frame, source, target,
                CommonEntitySerde.INSTANCE.fromLong(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static float returns(float value, int source, int target, long frame) {
        if (target == 0) {
            Frame thisFrame = elementStack.get().pop();
            assert thisFrame.frameId == frame;
        }

        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RETURN, frame, source, target,
                CommonEntitySerde.INSTANCE.fromFloat(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static double returns(double value, int source, int target, long frame) {
        if (target == 0) {
            Frame thisFrame = elementStack.get().pop();
            assert thisFrame.frameId == frame;
        }

        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RETURN, frame, source, target,
                CommonEntitySerde.INSTANCE.fromDouble(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static Object returns(Object value, int source, int target, long frame) {
        if (target == 0) {
            Frame thisFrame = elementStack.get().pop();
            assert thisFrame.frameId == frame;
        }

        final MonitoringEntities.MonitoringEvent msg = createEvent(
                MonitoringEntities.MonitoringEventType.RETURN, frame, source, target,
                CommonEntitySerde.INSTANCE.fromReference(value)
        );
        dao.reportEvent(msg);
        return value;
    }

    public static void returnsV(int source, int target, long frame) {
        if (target == 0) {
            Frame thisFrame = elementStack.get().pop();
            assert thisFrame.frameId == frame;
        }

        final MonitoringEntities.MonitoringEvent msg = createEvent(MonitoringEntities.MonitoringEventType.RETURN, frame, source, target);
        dao.reportEvent(msg);
    }

    public static void except(int source, long frame) {
        Frame thisFrame = elementStack.get().pop();
        assert thisFrame.frameId == frame;

        final MonitoringEntities.MonitoringEvent msg = createEvent(MonitoringEntities.MonitoringEventType.EXCEPT, frame, source, 0);
        dao.reportEvent(msg);
    }
}
