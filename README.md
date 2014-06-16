rest-test-dsl
=============

- it is a test DSL for restful client and server based on moco and dispatch. (since scala2.10.4)

- it also supports actor mode (will support Akka actor later) to make scalacheck style test cases more fast

- it is very simple to use the DSL to test REST client and server
    
    - as a client example: "client ask_for resource to DELETE by request should SUCCESS and_with {} "
    
    - as aserver example: "server own resource when DELETE given request then {} "
    
- there are some samples in the unit test folder    


