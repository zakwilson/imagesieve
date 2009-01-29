(ns imagesieve.logic
  (:use imagesieve.image clojure.parallel zutil.util)
  (:import (java.io File)
           (javax.imageio ImageIO)))


(in-ns 'imagesieve.logic)

(defn image-format [filename]
  (some #(re-find (re-pattern %) filename)
        (ImageIO/getReaderFormatNames)))

(defn process-file [op infile outfile]
  (try
   (let [img (read-image infile)]
     (when-not (nil? img)
       (write-image (op img) (image-format infile) (unique-filename outfile))))
   (catch Exception e nil)))

(defn process-dir [op indir outdir]
  (let [id (File. indir)
        od (File. outdir)
        ls (.list id)
        inpath (.getAbsolutePath id)
        outpath (.getAbsolutePath od)]
    (when-not (.exists od)
      (.mkdir od))
    (pmap #(process-file op
                         (str inpath (File/separator) %)
                         (str outpath (File/separator) %)) ls)))