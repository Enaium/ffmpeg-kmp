/*
 * Minimal Windows jni_md.h for cross-compiling the JNI bridge from Linux.
 * Mirrors the JDK's include/win32/jni_md.h (Classpath exception); the Linux
 * JDK only ships the linux variant, but the JNI functions are platform-neutral.
 */
#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#ifndef JNIEXPORT
#define JNIEXPORT __declspec(dllexport)
#endif
#ifndef JNIIMPORT
#define JNIIMPORT __declspec(dllimport)
#endif
#define JNICALL __stdcall

typedef long jint;
typedef __int64 jlong;
typedef signed char jbyte;

#endif /* _JAVASOFT_JNI_MD_H_ */
