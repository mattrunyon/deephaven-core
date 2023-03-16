/**
 * Copyright (c) 2016-2022 Deephaven Data Labs and Patent Pending
 */
package io.deephaven.javascript.proto.dhinternal.io.deephaven.proto.console_pb;

import elemental2.core.JsArray;
import elemental2.core.Uint8Array;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

@JsType(
        isNative = true,
        name = "dhinternal.io.deephaven.proto.console_pb.SignatureInformation",
        namespace = JsPackage.GLOBAL)
public class SignatureInformation {
    @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
    public interface ToObjectReturnType {
        @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
        public interface ParametersListFieldType {
            @JsOverlay
            static SignatureInformation.ToObjectReturnType.ParametersListFieldType create() {
                return Js.uncheckedCast(JsPropertyMap.of());
            }

            @JsProperty
            String getDocumentation();

            @JsProperty
            String getLabel();

            @JsProperty
            void setDocumentation(String documentation);

            @JsProperty
            void setLabel(String label);
        }

        @JsOverlay
        static SignatureInformation.ToObjectReturnType create() {
            return Js.uncheckedCast(JsPropertyMap.of());
        }

        @JsProperty
        double getActiveParameter();

        @JsProperty
        String getDocumentation();

        @JsProperty
        String getLabel();

        @JsProperty
        JsArray<SignatureInformation.ToObjectReturnType.ParametersListFieldType> getParametersList();

        @JsProperty
        void setActiveParameter(double activeParameter);

        @JsProperty
        void setDocumentation(String documentation);

        @JsProperty
        void setLabel(String label);

        @JsProperty
        void setParametersList(
                JsArray<SignatureInformation.ToObjectReturnType.ParametersListFieldType> parametersList);

        @JsOverlay
        default void setParametersList(
                SignatureInformation.ToObjectReturnType.ParametersListFieldType[] parametersList) {
            setParametersList(
                    Js.<JsArray<SignatureInformation.ToObjectReturnType.ParametersListFieldType>>uncheckedCast(
                            parametersList));
        }
    }

    @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
    public interface ToObjectReturnType0 {
        @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
        public interface ParametersListFieldType {
            @JsOverlay
            static SignatureInformation.ToObjectReturnType0.ParametersListFieldType create() {
                return Js.uncheckedCast(JsPropertyMap.of());
            }

            @JsProperty
            String getDocumentation();

            @JsProperty
            String getLabel();

            @JsProperty
            void setDocumentation(String documentation);

            @JsProperty
            void setLabel(String label);
        }

        @JsOverlay
        static SignatureInformation.ToObjectReturnType0 create() {
            return Js.uncheckedCast(JsPropertyMap.of());
        }

        @JsProperty
        double getActiveParameter();

        @JsProperty
        String getDocumentation();

        @JsProperty
        String getLabel();

        @JsProperty
        JsArray<SignatureInformation.ToObjectReturnType0.ParametersListFieldType> getParametersList();

        @JsProperty
        void setActiveParameter(double activeParameter);

        @JsProperty
        void setDocumentation(String documentation);

        @JsProperty
        void setLabel(String label);

        @JsProperty
        void setParametersList(
                JsArray<SignatureInformation.ToObjectReturnType0.ParametersListFieldType> parametersList);

        @JsOverlay
        default void setParametersList(
                SignatureInformation.ToObjectReturnType0.ParametersListFieldType[] parametersList) {
            setParametersList(
                    Js.<JsArray<SignatureInformation.ToObjectReturnType0.ParametersListFieldType>>uncheckedCast(
                            parametersList));
        }
    }

    public static native SignatureInformation deserializeBinary(Uint8Array bytes);

    public static native SignatureInformation deserializeBinaryFromReader(
            SignatureInformation message, Object reader);

    public static native void serializeBinaryToWriter(SignatureInformation message, Object writer);

    public static native SignatureInformation.ToObjectReturnType toObject(
            boolean includeInstance, SignatureInformation msg);

    public native ParameterInformation addParameters();

    public native ParameterInformation addParameters(ParameterInformation value, double index);

    public native ParameterInformation addParameters(ParameterInformation value);

    public native void clearActiveParameter();

    public native void clearDocumentation();

    public native void clearParametersList();

    public native int getActiveParameter();

    public native String getDocumentation();

    public native String getLabel();

    public native JsArray<ParameterInformation> getParametersList();

    public native boolean hasActiveParameter();

    public native boolean hasDocumentation();

    public native Uint8Array serializeBinary();

    public native void setActiveParameter(int value);

    public native void setDocumentation(String value);

    public native void setLabel(String value);

    public native void setParametersList(JsArray<ParameterInformation> value);

    @JsOverlay
    public final void setParametersList(ParameterInformation[] value) {
        setParametersList(Js.<JsArray<ParameterInformation>>uncheckedCast(value));
    }

    public native SignatureInformation.ToObjectReturnType0 toObject();

    public native SignatureInformation.ToObjectReturnType0 toObject(boolean includeInstance);
}
