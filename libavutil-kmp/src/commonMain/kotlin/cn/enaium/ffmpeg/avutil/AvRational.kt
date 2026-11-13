/*
 * Copyright (c) 2026 Enaium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package cn.enaium.ffmpeg.avutil

/** Rational number (`AVRational`), the unit of FFmpeg timestamps. */
public data class AvRational(
    public val numerator: Int,
    public val denominator: Int,
) {
    public fun toDouble(): Double =
        if (denominator == 0) 0.0 else numerator.toDouble() / denominator.toDouble()

    public val inverse: AvRational get() = AvRational(denominator, numerator)

    public companion object {
        public val ZERO: AvRational = AvRational(0, 1)
        public val ONE: AvRational = AvRational(1, 1)

        /** `av_make_q`. */
        public fun make(numerator: Int, denominator: Int): AvRational =
            AvRational(numerator, denominator)

        /** `av_inv_q`. */
        public fun invert(value: AvRational): AvRational =
            AvRational(value.denominator, value.numerator)
    }
}
