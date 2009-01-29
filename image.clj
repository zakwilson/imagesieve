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

