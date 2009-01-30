(ns imagesieve.logic
  (:use imagesieve.image zutil.util)
  (:import (java.io File)
           (javax.imageio ImageIO)))

(if (> 2 (.availableProcessors (Runtime/getRuntime)))
  (def mapfn map)
  (def mapfn pmap))

(in-ns 'imagesieve.logic)

(defn image-format [filename]
  (some #(re-find (re-pattern %) filename)
        (ImageIO/getReaderFormatNames)))

(defn process-file [op infile outfile]
  (try
   (let [img (read-image infile)]
     (when-not (nil? img)
       (op img)
       (write-image img (image-format infile) (unique-filename outfile))))
   (catch Exception e nil)
   (catch java.lang.OutOfMemoryError e
     (throw (Exception. "Sorry, we seem to have used up all your memory. Close some programs and try again.")))))

(defn process-dir [op indir outdir]
  (let [id (File. indir)
        od (File. outdir)
        ls (.list id)
        inpath (.getAbsolutePath id)
        outpath (.getAbsolutePath od)]
    (when-not (.exists od)
      (.mkdir od))
    (mapfn #(process-file op
                         (str inpath (File/separator) %)
                         (str outpath (File/separator) %)) ls)))