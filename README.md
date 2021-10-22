# Job Coin Mixer
Implementation of a rudimentary crypto mixer for Gemini

## Development
The application consists of a single REST service that serves requests for creating new mixer deposit addresses. 
It also runs two workers periodically to keep the mixer in sync
* A JobCoinDepositedWorker which watches for new deposits to our created deposit addresses. It aggregates these and 
moves the coin into the mixer house account
* A JobCoinMixerWorker which watches for new deposits to our house account and doles those coins out to the configured return addresses

### Running
There are two ways of running the service

### Annotation processing
I decied to use lombok to avoid a lot of boiler-plate for the models used here. In order for this to work in IntelliJ,
you will need the lombok plugin installed, and annotation processors enabled. See https://www.baeldung.com/lombok-ide 
for details. 

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
