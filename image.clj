;   Copyright (c) Zachary Wilson. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file COPYING at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns imagesieve.image
  (:import (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io File)))

(in-ns 'imagesieve.image)

(defn read-image [path]
  "Attempt to read the file at path in to a BufferedImage."
  (ImageIO/read (File. path)))

(defn write-image [img format path]
  "Write BufferedImage img to path using the specified format.
   Valid formats are: jpg, BMP, bmp, JPG, jpeg, wbmp, png, JPEG,
    PNG, WBMP, GIF, gif"
  (ImageIO/write img format (File. path)))

; The unchecked math and coercions here are ugly, but 6x faster.
(defn grey-px [px]
  "Turns a 32-bit ARGB pixel grey using simple averaging."
  (let [px (int (bit-and 0x00ffffff px)) ; ignore alpha, get correct values for brighter px
         r (bit-and (unchecked-divide px (int 65536)) (int 0xff))
         g (bit-and (unchecked-divide px (int 256)) (int 0xff))
         b (bit-and px (int 0xff))
         avg (unchecked-divide (unchecked-add (unchecked-add r g) b) (int 3))]
     (unchecked-add (unchecked-multiply avg (int 65536))
                    (unchecked-add (unchecked-multiply avg (int 256))
                                   avg))))

; The type hint really speeds things up by about 6x.
; Returning true instead of the image gives about a 10% speedup.
; Tried coercing x and y to ints. It's slightly faster not to.
; Inlining provides a slight speedup.
(definline pixel-filter! [#^BufferedImage img op]
  `(dotimes [y# (.getHeight ~img)]
     (dotimes [x# (.getWidth ~img)]
       (.setRGB ~img x# y#
                (~op (.getRGB ~img x# y#)))))
  true)

(defn make-grey! [#^BufferedImage img]
  (pixel-filter! img grey-px))