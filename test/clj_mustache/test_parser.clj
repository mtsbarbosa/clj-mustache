(ns clj-mustache.test-parser
  (:require
   [clj-mustache.parser :refer [extract-mustache-variables render
                                render-resource]]
   [clojure.string :as str])
  (:use clojure.test))

(deftest test-render-simple
  (is (= "Hello, Felix" (render "Hello, {{name}}" {:name "Felix"}))))

(deftest test-render-with-dollar-sign
  (is (= "Hello, $Felix!" (render "Hello, {{! This is a comment.}}{{name}}!"
                                  {:name "$Felix"}))))

(deftest test-render-multi-line
  (is (= "Hello\nFelix" (render "Hello\n{{name}}" {:name "Felix"}))))

(deftest test-nil-variable
  (is (= "Hello, " (render "Hello, {{name}}" {:name nil}))))

(deftest test-missing-variables
  (is (= "Hello, . " (render "Hello, {{name}}. {{{greeting}}}" {}))))

(deftest test-render-html-unescaped
  (is (= "&\\\"<>"
         (render "{{{string}}}" {:string "&\\\"<>"}))))

(deftest test-render-html-unescaped-ampersand
  (is (= "&\"<>"
         (render "{{&string}}" {:string "&\"<>"}))))

(deftest test-render-html-escaped
  (is (= "&amp;&quot;&lt;&gt;&apos;"
         (render "{{string}}" {:string "&\"<>'"}))))

(deftest test-render-list
  (is (= "Hello, Felix, Jenny!" (render "Hello{{#names}}, {{name}}{{/names}}!"
                                        {:names [{:name "Felix"}
                                                 {:name "Jenny"}]}))))

(deftest test-render-list-twice
  (is (= "Hello, Felix, Jenny! Hello, Felix, Jenny!"
         (render (str "Hello{{#names}}, {{name}}{{/names}}! "
                      "Hello{{#names}}, {{name}}{{/names}}!")
                 {:names [{:name "Felix"} {:name "Jenny"}]}))))

(deftest test-render-single-value
  (is (= "Hello, Felix!" (render "Hello{{#person}}, {{name}}{{/person}}!"
                                 {:person {:name "Felix"}}))))

(deftest test-render-seq
  (is (= "Hello, Felix, Jenny!" (render "Hello{{#names}}, {{name}}{{/names}}!"
                                        {:names (seq [{:name "Felix"}
                                                      {:name "Jenny"}])}))))

(deftest test-render-hash
  ; according to mustache(5) non-false, non-list value
  ; should be used as a context for a single rendering of a block
  (is (= "Hello, Felix!" (render "Hello{{#person}}, {{name}}{{/person}}!"
                                 {:person {:name "Felix"}}))))

(deftest test-render-empty-list
  (is (= "" (render "{{#things}}Something{{/things}}" {:things []}))))

(deftest test-render-nested-list
  (is (= "z" (render "{{#x}}{{#y}}{{z}}{{/y}}{{/x}}" {:x {:y {:z "z"}}}))))

(deftest test-render-comment
  (is (= "Hello, Felix!" (render "Hello, {{! This is a comment.}}{{name}}!"
                                 {:name "Felix"}))))

(deftest test-render-tags-with-whitespace
  (is (= "Hello, Felix" (render "Hello, {{# names }}{{ name }}{{/ names }}"
                                {:names [{:name "Felix"}]}))))

(deftest test-render-boolean-true
  (is (= "Hello, Felix" (render "Hello, {{#condition}}Felix{{/condition}}"
                                {:condition true})))
  (is (= "Hello, Felix that is the condition:true!" (render "Hello, {{#condition}}Felix that is the condition:{{condition}}!{{/condition}}"
                                                            {:condition true})))
  (is (= "Hello, Felix that is the condition:true!" (render "Hello, {{#attr/condition}}Felix that is the condition:{{attr/condition}}!{{/attr/condition}}"
                                                            {:attr/condition true})))
  (is (= "Hello 2, Tom that is the condition:true!" (render "Hello 2, {{#attr/condition-b}}Tom that is the condition:{{attr/condition-b}}!{{/attr/condition-b}}{{^attr/condition-b}}Ooops{{/attr/condition-b}}"
                                                            {:attr/condition-b true})))
  (is (= "Hello, Felix that is the condition:test!" (render "Hello, {{#attr/condition-a}}Felix that is the condition:{{attr/condition-a}}!{{/attr/condition-a}}{{^attr/condition-a}}Ooops{{/attr/condition-a}}"
                                                            {:attr/condition-a "test"})))
  (is (= "Hello Fulano"
         (clj-mustache.parser/render "{{#customer/preferred-name}}Hello {{customer/preferred-name}}{{/customer/preferred-name}}{{^customer/preferred-name}}Hello customer{{/customer/preferred-name}}"
                                     {:customer/preferred-name "Fulano", :account/current-interest-rate 43, :account/general-late-fee-fixed 110, :account/general-late-fee-monthly 3}))))

(deftest test-render-boolean-false
  (is (= "Hello, " (render "Hello, {{#condition}}Felix{{/condition}}"
                           {:condition false}))))

(deftest test-render-inverted-empty-list
  (is (= "Empty" (render "{{^things}}Empty{{/things}}" {:things []}))))

(deftest test-render-inverted-list
  (is (= "" (render "{{^things}}Empty{{/things}}" {:things ["Something"]}))))

(deftest test-render-inverted-boolean-true
  (is (= "Hello, " (render "Hello, {{^condition}}Felix{{/condition}}"
                           {:condition true}))))

(deftest test-render-inverted-boolean-false
  (is (= "Hello, Felix" (render "Hello, {{^condition}}Felix{{/condition}}"
                                {:condition false}))))

(deftest test-render-with-delimiters
  (is (= "Hello, Felix" (render "{{=<% %>=}}Hello, <%name%>" {:name "Felix"}))))

(deftest test-render-with-regex-delimiters
  (is (= "Hello, Felix" (render "{{=[ ]=}}Hello, [name]" {:name "Felix"}))))

(deftest test-render-with-delimiters-changed-twice
  (is (= "Hello, Felix" (render "{{=[ ]=}}[greeting], [=<% %>=]<%name%>"
                                {:greeting "Hello" :name "Felix"}))))

(deftest test-render-tag-with-dotted-name-like-section
  (is (= "Hello, Felix" (render "Hello, {{felix.name}}"
                                {:felix {:name "Felix"}}))))

(deftest test-render-lambda
  (is (= "Hello, Felix" (render "Hello, {{name}}"
                                {:name (fn [] "Felix")}))))

(deftest test-render-lambda-with-params
  (is (= "Hello, Felix" (render "{{#greet}}Felix{{/greet}}"
                                {:greet #(str "Hello, " %)})))
  (is (= "Hi TOM Hi BOB "
         (render "{{#people}}Hi {{#upper}}{{name}}{{/upper}} {{/people}}"
                 {:people [{:name "Tom"}, {:name "Bob"}]
                  :upper (fn [text] (fn [render-fn] (str/upper-case (render-fn text))))}))))

(deftest test-render-resource-template
  (is (= "Hello, Felix" (render-resource "templates/hello.mustache" {:name "Felix"}))))

(deftest test-render-with-partial
  (is (= "Hi, Felix" (render "Hi, {{>name}}" {:n "Felix"} {:name "{{n}}"}))))

(deftest test-render-partial-recursive
  (is (= "One Two Three Four Five" (render "One {{>two}}"
                                           {}
                                           {:two "Two {{>three}}"
                                            :three "Three {{>four}}"
                                            :four "Four {{>five}}"
                                            :five "Five"}))))

(deftest test-render-with-variable-containing-template
  (is (= "{{hello}},world" (render "{{tmpl}},{{hello}}" {:tmpl "{{hello}}" :hello "world"}))))

(deftest test-render-sorted-set
  (let [sort-by-x (fn [x y] (compare (:x x) (:x y)))
        l (sorted-set-by sort-by-x {:x 1} {:x 5} {:x 3})]
    (is (= "135" (render "{{#l}}{{x}}{{/l}}" {:l l})))
    (is (= "" (render "{{^l}}X{{/l}}" {:l l}))))
  (is (= "X" (render "{{^l}}X{{/l}}" {:l (sorted-set)}))))

(deftest test-path-whitespace-handled-consistently
  (is (= (render "{{a}}" {:a "value"}) "value"))
  (is (= (render "{{ a }}" {:a "value"}) "value"))
  (is (= (render "{{a.b}}" {:a {:b "value"}}) "value"))
  (is (= (render "{{ a.b }}" {:a {:b "value"}}) "value")))

(deftest test-extract-mustache-variables
  (is (= '("names")
         (extract-mustache-variables "{{#names}}\n    <li>{{.}}</li>\n{{/names}}")))
  (is (= '()
         (extract-mustache-variables "<h2>Felix' section<h2>\n{{! Look ma, I've written a section }}")))
  (is (= '("ignore")
         (extract-mustache-variables "{{^ignore}}Hello, World!{{/ignore}}")))
  (is (= '("name" "people")
         (extract-mustache-variables "<ul>\n{{#people}}\n    <li>{{name}}</li>\n{{/people}}\n</ul>")))
  (is (= '()
         (extract-mustache-variables "Hello {{.}}")))
  (is (= '("person/first-name")
         (extract-mustache-variables "Hello {{person/first-name}}")))
  (is (= '("person/first-name")
         (extract-mustache-variables "Hello {{#person/first-name}}{{person/first-name}}{{/person/first-name}}"))))
