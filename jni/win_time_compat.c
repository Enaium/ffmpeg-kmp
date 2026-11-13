/*
 * MinGW compatibility aliases for Kotlin/Native.
 *
 * FFmpeg is compiled with a recent mingw-w64 headers set, where the CRT
 * exposes the 64-bit-time entry points. Kotlin/Native links against an older
 * winpthreads import library that has the ordinary entry points but not the
 * aliases. Keep the aliases out of the JNI DLL (it links the newer CRT);
 * only the merged static archive includes this translation unit.
 */

#if defined(_WIN32)

typedef long long fk_time_t;

struct fk_timespec {
    fk_time_t tv_sec;
    long tv_nsec;
};

extern int clock_gettime(int clock_id, struct fk_timespec *tp);
extern int nanosleep(const struct fk_timespec *request, struct fk_timespec *remain);

int clock_gettime64(int clock_id, struct fk_timespec *tp) {
    return clock_gettime(clock_id, tp);
}

int nanosleep64(const struct fk_timespec *request, struct fk_timespec *remain) {
    return nanosleep(request, remain);
}

#endif
