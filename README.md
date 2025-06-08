# Milquetoast

Milquetoast is a Clojure library for interacting with MQTT brokers. It provides a simple and intuitive API for sending and receiving messages, subscribing to topics, and managing MQTT connections.

## Usage

First, connect to the MQTT broker:

```clojure
(def client (milquetoast.api/connect! :host "localhost" :port 1883))
```

To send a message:

```clojure
(milquetoast.api/send! client "test/topic" "Hello, MQTT!")
```

To subscribe to a topic and receive messages:

```clojure
(def chan (milquetoast.api/subscribe! client "test/topic"))
```

You can then take messages from the channel as they arrive. Here's an example of how to listen on the channel and print the messages:

```clojure
(go-loop []
  (when-let [msg (<! chan)]
    (println "Received message:" msg)
    (recur)))
```

To disconnect from the broker:

```clojure
(milquetoast.api/stop! client)
```

For more detailed usage, see the API documentation and the example code in the `examples` directory.

## JSON Client

Milquetoast also provides a JSON client, which automatically parses JSON payloads from received messages and serializes JSON payloads for sent messages. 

To connect using the JSON client:

```clojure
(def json-client (milquetoast.api/connect-json! :host "localhost" :port 1883))
```

The JSON client can be used in the same way as the regular client, but note that the payload of received messages will be a map (parsed from the JSON payload), and the payload of sent messages should be a map (which will be serialized to a JSON string).
```
