# Transafe
This application is made for a hackathon competition as demo. The aim of the app is transferring important files by using blockchain

### Setup your development via the guide link 
Make sure you have right development setup before start running

https://docs.r3.com/en/platform/corda/4.8/open-source/getting-set-up.html

## Running the sample
Deploy and run the nodes by:
```
./gradlew deployNodes
./build/nodes/runnodes
```

If you don't have proper java 8 version as main you can configure and use below jar command in terminal under the ".\build\nodes" folder.
```
C:\Program" "Files\Java\jdk1.8.0_333\bin\java -jar .\runnodes.jar
```
Then you will need to also start the spring server for the Cordapp by running the following commands seperately: 
`./gradlew bootRunNode1`will have the Node1 server running on 8080 port 
, and `./gradlew bootRunNode2`will start the Node2 server on 8090 port

## Operating the Cordapp
Now go to postman and excute the following in order: (All of the API are POST request!)
1. Create an account on Node1 node: `http://localhost:8080/createAccount/IsBankasi` 
2. Create an account on Node2 node: `http://localhost:8090/createAccount/TurkTelekom`
3. IsBankasi now handshake with node 2: `http://localhost:8080/handShake` 
`
Body:
{
   "fromPerson": "IsBankasi",
   "targetNodeName": "Node2"
   }
`
4. TurkTelekom now handshake with node 1: `http://localhost:8090/handShake`
      `
      Body:
      {
      "fromPerson": "IsBankasi",
      "targetNodeName": "Node1"
      }
      `
5. Send a file from IsBankasi to TurkTelekom: `http://localhost:8080/sendFile`
`
Body:
   {
   "sender": "IsBankasi",
   "receiver": "TurkTelekom",
   "receiverHost": "O=Node2,L=New York,C=US,CN=Alice",
   "file": "Deneme dosyasi",
   "startDate": "2022-06-15 08:38:00",
   "endDate": "2022-06-20 14:00:00"
   }
`
6. Receive file: `http://localhost:8090/receiveFile`
`
Body:
   {
   "sender": "IsBankasi",
   "receiver": "TurkTelekom",
   "senderHost": "O=Node1,L=London,C=GB,CN=Bob"
   }
` 
 








