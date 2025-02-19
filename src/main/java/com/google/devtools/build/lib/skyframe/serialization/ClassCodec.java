// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.skyframe.serialization;

import static com.google.devtools.build.lib.skyframe.serialization.strings.UnsafeStringCodec.stringCodec;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import java.io.IOException;

/** Codec for {@link Class}. */
class ClassCodec extends LeafObjectCodec<Class<?>> {
  @SuppressWarnings("unchecked")
  @Override
  public Class<Class<?>> getEncodedClass() {
    return (Class<Class<?>>) (Object) Class.class;
  }

  @Override
  public void serialize(
      SerializationDependencyProvider dependencies, Class<?> obj, CodedOutputStream codedOut)
      throws SerializationException, IOException {
    codedOut.writeBoolNoTag(obj.isPrimitive());
    if (obj.isPrimitive()) {
      codedOut.writeInt32NoTag(Preconditions.checkNotNull(PRIMITIVE_CLASS_INDEX_MAP.get(obj), obj));
    } else {
      stringCodec().serialize(dependencies, obj.getName(), codedOut);
    }
  }

  @Override
  public Class<?> deserialize(
      SerializationDependencyProvider dependencies, CodedInputStream codedIn)
      throws SerializationException, IOException {
    boolean isPrimitive = codedIn.readBool();
    if (isPrimitive) {
      return PRIMITIVE_CLASS_INDEX_MAP.inverse().get(codedIn.readInt32());
    }
    String className = stringCodec().deserialize(dependencies, codedIn);
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new SerializationException("Couldn't find class for " + className, e);
    }
  }

  private static final ImmutableBiMap<Class<?>, Integer> PRIMITIVE_CLASS_INDEX_MAP =
      ImmutableBiMap.<Class<?>, Integer>builder()
          .put(byte.class, 1)
          .put(short.class, 2)
          .put(int.class, 3)
          .put(long.class, 4)
          .put(char.class, 5)
          .put(float.class, 6)
          .put(double.class, 7)
          .put(boolean.class, 8)
          .put(void.class, 9)
          .buildOrThrow();
}
