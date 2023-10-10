/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.handler;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayOutputStream;

/**
 * Kryo redis serializer
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class KryoRedisSerializer<T> implements RedisSerializer<T> {

    private static final byte[] EMPTY_ARRAY = new byte[0];

    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(Kryo::new);

    private final Class<T> type;

    private Kryo getKryo() {
        Kryo kryo = KRYO_THREAD_LOCAL.get();
        kryo.setReferences(false);
        kryo.setRegistrationRequired(false);
        kryo.register(type);
        return kryo;
    }

    public KryoRedisSerializer(Class<T> type) {
        super();
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return EMPTY_ARRAY;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); Output output = new Output(baos)) {
            getKryo().writeClassAndObject(output, t);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new SerializationException("Cannot serialize", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (Input input = new Input(bytes)) {
            return (T) getKryo().readClassAndObject(input);
        } catch (Exception e) {
            throw new SerializationException("Cannot deserialize", e);
        }
    }
}
