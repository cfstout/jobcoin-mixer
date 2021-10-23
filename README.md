# Job Coin Mixer
Implementation of a rudimentary crypto mixer for Gemini

## Development
The application consists of a single REST service that serves requests for creating new mixer deposit addresses. 
It also runs two workers periodically to keep the mixer in sync
* A JobCoinDepositedWorker which watches for new deposits to our created deposit addresses. It aggregates these and 
moves the coin into the mixer house account
* A JobCoinMixerWorker which watches for new deposits to our house account and doles those coins out to the configured return addresses

### Running
There are two ways of running the service:
* There is a run configuration stored in `.idea/runConfigurations`. If you import the project into intellij, you should be
 able to refer to this configuration to run the application.
* You can also run via gradle: `./gradlew run --args "server config.yaml"`

### Annotation processing
I decied to use lombok to avoid a lot of boiler-plate for the models used here. In order for this to work in IntelliJ,
you will need the lombok plugin installed, and annotation processors enabled. See https://www.baeldung.com/lombok-ide 
for details. 

## Example
To run the mixer and see it in action
1. Start the application as described in [running](#running)
2. Curl the mixer to get a deposit address for your provided payout wallets:
  ```bash
  curl -XPOST localhost:8080/mixer/set-up -d '{"returnAddresses": ["payout1", "payout2"]}' -H 'Content-Type: application/json'
  ```
3. This will return a response like `{"depositAddress":"e065dd95-daab-4d54-9f53-f73e360f285b"}`. This is the address that you will need to send job coin to.
4. Go to the UI: [https://jobcoin.gemini.com/amazingly-yearning](https://jobcoin.gemini.com/amazingly-yearning)
5. Mint coins to a new address you own via the `Create 50 Coins` button
6. Send the coins from your newly minted address to the deposit address in step 3. 
7. Wait about 60s to allow the workers to run. You should see lines in the application logs such as "Sending from deposit addresses to the house account" and "Paying out to configured payout addresses" to indicate work being done.
8. Refresh the UI, and you should see your coins paid out to the configured address, minus a small fee ;) 

## Assumptions
* We assume that we will never see transactions returned by the API older than transactions previously seen. IOW if
the api returns a transaction at time T+2, we will never later poll the API to see a new transaction that happened at 
time T+1. In real life this might not always be the case, but with a block chain I believe we shoud be able to swap out
actual times for block numbers to get the same behavior. 

## Next Steps
### Prepare for production
* Add a database for persistence
* Ensure consistency in a distributed environment
### Other improvements
* Round-robin payouts so first address in the list doesn't get the most deposits
### CI/CD
* Run tests on commit
* Dockerize the app for running in a deployed environment
