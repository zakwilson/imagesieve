;   Copyright (c) Zachary Wilson. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file COPYING at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns imagesieve.image
  (:import (java.awt.image  BufferedImage)
           (javax.imageio ImageIO)
           (java.io File)))

(in-ns 'imagesieve.image)

(defn read-image [path]
  (ImageIO/read (File. path)))

(defn write-image [img format path]
  (ImageIO/write img format (File. path)))

(defn grey-px [px]
  (let [px (int (bit-and 0x00ffffff px)) ;ignore alpha, get correct values for brighter px
        r (bit-and (unchecked-divide px (int 65536)) (int 0xff))
        g (bit-and (unchecked-divide px (int 256)) (int 0xff))
        b (bit-and px (int 0xff))
        avg (unchecked-divide (unchecked-add (unchecked-add r g) b) (int 3))]
    (unchecked-add (unchecked-multiply avg (int 65536))
                   (unchecked-add (unchecked-multiply avg (int 256))
                                  avg))))

(defn make-grey! [#^BufferedImage img]
  (dotimes [y (.getHeight img)]
    (dotimes [x (.getWidth img)]
      (let [ix (int x)
            iy (int y)]
        (.setRGB img ix iy
                 (grey-px (.getRGB img ix iy))))))
  img)

