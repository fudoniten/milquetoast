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

You can then take messages from the channel as they arrive.

To disconnect from the broker:

```clojure
(milquetoast.api/stop! client)
```

For more detailed usage, see the API documentation and the example code in the `examples` directory.
