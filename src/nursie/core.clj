(ns nursie.core
  (:use [endophile.core :only [mp to-clj]])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as en])
  (:gen-class))

(defn parse-post
  [file]
  (let [[_ yyyy mm dd hh mi slug]
        (re-matches #"(\d{4})(\d\d)(\d\d)T(\d\d)(\d\d)Z-(.+)\.[^.]+"
                    (.getName file))
        nodes (-> file slurp mp to-clj)]
    {:yyyy yyyy :mm mm :dd dd :hh hh :mi mi
     :slug slug
     :permalink (str "/" slug)
     :datetime (str yyyy "-" mm "-" dd "T" hh ":" mi "Z")
     :nodes nodes
     :title (->> (en/select nodes [:h1]) (map :content) first)}))
 
(defn get-link [post]
  (select-keys post [:permalink :title]))

(defn backlinks [posts]
  (letfn [(backlinked? [target]
            (fn [source]
              (seq
                (en/select (:nodes source)
                           [[:a (en/attr= :href (:permalink target))]]))))]
    (for [p posts]
      (assoc p :backlinks (->> posts
                            (filter (backlinked? p))
                            (map get-link))))))

(defn prev-posts [posts]
  (map #(assoc %1 :prev (get-link %2))
       posts
       (-> posts rest vec (conj {}))))

(defn next-posts [posts]
  (let [posts (reverse posts)]
    (reverse (map #(assoc %1 :next (get-link %2))
                  posts
                  (-> posts rest vec (conj {}))))))

(en/deftemplate page "template.html" [post]
  [:title] (en/prepend (:title post))
  [:bogon] (en/substitute (:nodes post))
  [:h1] (en/after (en/html [:time {:datetime (:datetime post)}]))
  [:.backlinks] (when (seq (:backlinks post)) identity)
  [:.backlinks :li] (en/clone-for [b (:backlinks post)]
                      [:a] (en/do->
                             (en/set-attr :href (:permalink b))
                             (en/content  (:title b))))
  
  )


(defn html [post]
  (assoc post :html (apply str (page post))))

(defn build-posts [files]
  (->> files
    (map parse-post)
    (sort-by :datetime)
    reverse
    backlinks
    prev-posts
    next-posts
    (map html)))

(defn write-html [srcdir post]
  (spit (str srcdir "/site" (:permalink post) ".html") (:html post)))

(defn run [srcdir]
  (->> (.listFiles (io/file srcdir "posts"))
    build-posts
    (map (partial write-html srcdir))))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")) 
