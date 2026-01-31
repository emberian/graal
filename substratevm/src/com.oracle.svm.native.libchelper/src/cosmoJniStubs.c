/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * JNI stub implementations for Cosmopolitan libc on macOS (darwin).
 *
 * These stubs provide minimal implementations of JNI native methods from
 * the JDK's libnet and libnio libraries. The original implementations make
 * JNI calls during JNI_OnLoad which fails on cosmo/macOS because the JNI
 * environment is not fully initialized at that point.
 *
 * These stubs should be linked with higher priority than the original
 * implementations (using --whole-archive or symbol weakening).
 *
 * Usage:
 *   1. Compile with cosmocc: aarch64-unknown-cosmo-cc -c cosmoJniStubs.c
 *   2. Either create a separate library or add to liblibchelper.a
 *   3. Link with --whole-archive to ensure symbols override originals
 */

#if defined(__COSMOPOLITAN__)

#include <stdint.h>
#include <string.h>
#include <pthread.h>
#include <signal.h>

/* JNI type definitions */
typedef void* JNIEnv;
typedef void* JavaVM;
typedef void* jclass;
typedef void* jobject;
typedef void* jstring;
typedef void* jbyteArray;
typedef int32_t jint;
typedef int64_t jlong;
typedef uint8_t jboolean;

/* JNI version constant */
#define JNI_VERSION_1_8 0x00010008

/*
 * libnet stubs - JNI_OnLoad and platform initialization
 */
jint JNI_OnLoad_net(JavaVM *vm, void *reserved) {
    (void)vm;
    (void)reserved;
    return JNI_VERSION_1_8;
}

jint JNI_OnLoad_dynamic_net(JavaVM *vm, void *reserved) {
    (void)vm;
    (void)reserved;
    return JNI_VERSION_1_8;
}

int NET_PlatformInit(void) {
    return 0;
}

void NET_ThrowByNameWithLastError(JNIEnv *env, const char *name, const char *defaultDetail) {
    (void)env;
    (void)name;
    (void)defaultDetail;
}

void NET_ThrowNew(JNIEnv *env, int errorNum, char *msg) {
    (void)env;
    (void)errorNum;
    (void)msg;
}

jboolean NET_IsIPv4Available(void) {
    return 1;
}

jboolean NET_IsIPv6Available(void) {
    return 0;
}

jboolean NET_SockaddrEqualsInetAddress(JNIEnv *env, void *sa, jobject iaObj) {
    (void)env;
    (void)sa;
    (void)iaObj;
    return 0;
}

jobject NET_SockaddrToInetAddress(JNIEnv *env, void *sa, int *port) {
    (void)env;
    (void)sa;
    (void)port;
    return 0;
}

int NET_InetAddressToSockaddr(JNIEnv *env, jobject iaObj, int port, void *sa,
                               int *len, jboolean v4MappedAddress) {
    (void)env;
    (void)iaObj;
    (void)port;
    (void)sa;
    (void)len;
    (void)v4MappedAddress;
    return -1;
}

void SetSockOptL(JNIEnv *env, jobject fdo, jint opt, jobject on) {
    (void)env;
    (void)fdo;
    (void)opt;
    (void)on;
}

jobject GetSockOptL(JNIEnv *env, jobject fdo, jint opt) {
    (void)env;
    (void)fdo;
    (void)opt;
    return 0;
}

/*
 * libnio stubs - NIO channel and file operations
 */
jint JNI_OnLoad_nio(void* vm, void* reserved) {
    (void)vm;
    (void)reserved;
    return JNI_VERSION_1_8;
}

void Java_sun_nio_ch_IOUtil_initIDs(JNIEnv* env, jclass cls) {
    (void)env;
    (void)cls;
}

jint Java_sun_nio_ch_IOUtil_iovMax(JNIEnv* env, jclass cls) {
    (void)env;
    (void)cls;
    return 1024;
}

jint Java_sun_nio_ch_IOUtil_writevMax(JNIEnv* env, jclass cls) {
    (void)env;
    (void)cls;
    return 1024;
}

void Java_sun_nio_ch_NativeThread_init(JNIEnv* env, jclass cls) {
    (void)env;
    (void)cls;
}

jlong Java_sun_nio_ch_NativeThread_current0(JNIEnv* env, jclass cls) {
    (void)env;
    (void)cls;
    return (jlong)(uintptr_t)pthread_self();
}

void Java_sun_nio_ch_NativeThread_signal0(JNIEnv* env, jclass cls, jlong thread) {
    (void)env;
    (void)cls;
    pthread_kill((pthread_t)(uintptr_t)thread, SIGUSR1);
}

jint Java_sun_nio_ch_UnixFileDispatcherImpl_read0(JNIEnv* env, jclass cls,
                                                   jobject fdo, jlong address, jint len) {
    (void)env;
    (void)cls;
    (void)fdo;
    (void)address;
    (void)len;
    return -1;
}

jlong Java_sun_nio_ch_UnixFileDispatcherImpl_seek0(JNIEnv* env, jclass cls,
                                                    jobject fdo, jlong offset) {
    (void)env;
    (void)cls;
    (void)fdo;
    (void)offset;
    return -1;
}

jlong Java_sun_nio_ch_UnixFileDispatcherImpl_size0(JNIEnv* env, jclass cls, jobject fdo) {
    (void)env;
    (void)cls;
    (void)fdo;
    return -1;
}

jint Java_sun_nio_fs_UnixNativeDispatcher_init(JNIEnv* env, jclass cls) {
    (void)env;
    (void)cls;
    return 0;
}

jbyteArray Java_sun_nio_fs_UnixNativeDispatcher_getcwd(JNIEnv* env, jclass cls) {
    (void)env;
    (void)cls;
    return (jbyteArray)0;
}

jbyteArray Java_sun_nio_fs_UnixNativeDispatcher_strerror(JNIEnv* env, jclass cls, jint errnum) {
    (void)env;
    (void)cls;
    (void)errnum;
    return (jbyteArray)0;
}

/*
 * darwin platform stubs - macOS version info
 */
typedef struct {
    int major;
    int minor;
    int patch;
} OperatingSystemVersion;

OperatingSystemVersion operatingSystemVersion(void) {
    OperatingSystemVersion v = {0, 0, 0};
    return v;
}

const char* systemVersionPlatform(void) {
    return "cosmo";
}

const char* systemVersionPlatformFallback(void) {
    return "cosmo";
}

#endif /* __COSMOPOLITAN__ */
