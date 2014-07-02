(ns logkeeper.util
  (:import [java.io File]
           [com.google.common.base Splitter]))

(defonce separator File/separator)

(defonce splitter (-> (Splitter/on " ") (.trimResults) (.omitEmptyStrings)))

(defn mkdirs [path]
  (let [dir (File. path)]
    (when-not (.exists dir)
      (.mkdirs dir))))

(defn newfile [path]
  (let [file (File. path)]
    (if (.exists file)
      (when-not (.isFile file)
        (throw (Exception. (str path " is not a file!"))))
      (let [dir (->> (.lastIndexOf path File/separator)
                     (.substring path))]
        (mkdirs dir)
        (.createNewFile path)))))

(defn listfiles
  ([path]
   (listfiles path nil))
  ([path exclude-regex]
   (let [dir (File. path)]
     (when-not (.isDirectory dir)
       (throw (Exception. (str path " is not a directory!"))))
     (->> (.listFiles dir)
          (filter (complement #(or (.isDirectory %)
                                   (if-not (nil? exclude-regex)
                                     (re-find exclude-regex (.getName %))
                                     false))))))))

(defn meta-line? [line]
  (.startsWith line "{"))

(defn normal-line? [line]
  (-> (or (.isEmpty (.trim line))
          (.startsWith line "#")
          (meta-line? line))
      not))