package catchla.yep.util;

import android.support.v4.util.SimpleArrayMap;

import com.bluelinelabs.logansquare.JsonMapper;
import com.bluelinelabs.logansquare.LoganSquare;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import catchla.yep.model.ResponseCode;
import retrofit.Converter;

/**
 * Created by mariotaku on 15/12/1.
 */
public class LoganSquareConverterFactory extends Converter.Factory {

    private static final SimpleArrayMap<Type, Converter<ResponseBody, ?>> sResponseConverters = new SimpleArrayMap<>();

    static {
        sResponseConverters.put(ResponseCode.class, new ResponseCode.Converter());
    }

    @Override
    public Converter<?, RequestBody> toRequestBody(final Type type, final Annotation[] annotations) {
        if (type instanceof Class) {
            return new ObjectSerializeConverter((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
//            final Type rawType = ((ParameterizedType) type).getRawType();
//            if (rawType instanceof Class) {
//                final Class<?> rawCls = (Class<?>) rawType;
//                if (List.class.isAssignableFrom(rawCls)) {
//                    return new ListSerializeConverter(rawCls, ((ParameterizedType) type).getActualTypeArguments()[0]);
//                }
//            }
        }
        return null;
    }

    @Override
    public Converter<ResponseBody, ?> fromResponseBody(final Type type, final Annotation[] annotations) {
        final Converter<ResponseBody, ?> converter = sResponseConverters.get(type);
        if (converter != null) return converter;
        if (type instanceof Class) {
            return new ObjectParseConverter((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            final Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class) {
                final Class<?> rawCls = (Class<?>) rawType;
                if (List.class.isAssignableFrom(rawCls)) {
                    return new ListParseConverter(rawCls, ((ParameterizedType) type).getActualTypeArguments()[0]);
                }
            }
        }
        return null;
    }

    private class ObjectParseConverter implements Converter<ResponseBody, Object> {
        private final Class<?> cls;

        public ObjectParseConverter(final Class<?> cls) {
            this.cls = cls;
        }

        @Override
        public Object convert(final ResponseBody value) throws IOException {
            return LoganSquare.parse(value.byteStream(), cls);
        }
    }

    private class ListParseConverter implements Converter<ResponseBody, Object> {
        private final Class<?> rawCls;
        private final Type type;

        public ListParseConverter(final Class<?> rawCls, final Type type) {
            this.rawCls = rawCls;
            this.type = type;
        }

        @Override
        public Object convert(final ResponseBody value) throws IOException {
            if (rawCls.equals(List.class)) {
                return LoganSquare.parseList(value.byteStream(), (Class<?>) type);
            }
            //noinspection TryWithIdenticalCatches
            try {
                final List list = (List) rawCls.newInstance();
                //noinspection unchecked
                list.addAll(LoganSquare.parseList(value.byteStream(), (Class<?>) type));
                return list;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class ObjectSerializeConverter implements Converter<Object, RequestBody> {
        private final Class<?> type;

        public ObjectSerializeConverter(final Class<?> type) {
            this.type = type;
        }

        @Override
        public RequestBody convert(final Object value) throws IOException {
            final JsonMapper jsonMapper = LoganSquare.mapperFor(type);
            //noinspection unchecked
            return RequestBody.create(MediaType.parse("application/json"), jsonMapper.serialize(value));
        }
    }
}
