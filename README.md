# RAFT-Algorithm

## Technologies Used

- Java 21
- Maven

## Raft Consensus Algorithm

This section provides an overview of the Raft consensus algorithm. The algorithm is divided into nine sections, each describing a specific aspect of the protocol. Pseudo code is provided for each phase.

## Initialization

```shell
on initialization do
    currentTerm := 0
    votedFor := null
    log := ()
    commitLength := 0
    currentRole := follower
    currentLeader := null
    votesReceived := {}
    sentLength := {}
    ackedLength := {}
end on
```
The initialization phase sets the initial state of a node in the Raft cluster.

in the Raft cluster.

* `currentTerm` is set to 0, indicating no known term.
* `votedFor` is set to `null`, indicating the node has not voted for any candidate.
* `log` is an empty log.
* `commitLength` is set to 0, indicating no committed entries.
* `currentRole` is set to `follower`, indicating the node is a follower.
* `currentLeader` is set to `null`, indicating no known leader.
* `votesReceived` is an empty set of received votes.
* `sentLength`, ackedLength are empty dictionaries for tracking log replication progress.

## Recovery from Crash

```shell

on recovery from crash do
    currentRole := follower
    currentLeader := null
    votesReceived := {}
    sentLength := {}
    ackedLength := {}
end on
```

The recovery phase is triggered when a node restarts after a crash. It restores the node to its initial state.

* `currentRole` is set to `follower`.
* `currentLeader` is set to `null`.
* `votesReceived` is cleared.
* `sentLength` and `ackedLength` are cleared.

## Election and Suspected Leader
```shell

on node nodeld suspects leader has failed, or on election timeout do
    currentTerm := currentTerm + 1
    currentRole := candidate
    votedFor := nodeld
    votesReceived := {nodeld}
    lastTerm := 0
    if log.length > 0 then
        lastTerm := log[log.length - 1].term
    end if
    msg := (VoteRequest, nodeld, currentTerm, log.length, lastTerm)
    for each node in nodes do
        send msg to node
    end for
    start election timer
end on
```

This phase is triggered when a follower suspects the leader has failed or when an election timeout occurs.

* `currentTerm` is incremented.
* `currentRole` is set to `candidate`.
* `votedFor` is set to the node itself.
* `votesReceived` is initialized with the node's vote.
* `lastTerm` is determined by inspecting the last entry in the log.
* A `VoteRequest` message is sent to each node in the cluster.
* An election timer is started.

## Voting on a New Leader


```shell
on receiving (VoteRequest, cId, Term, cLogLength, cLogTerm) at node nodeld do
    if Term > currentTerm then
        currentTerm := Term
        currentRole := follower
        votedFor := null
    end if
    lastTerm := 0
    if log.length > 0 then
        lastTerm := log[log.length - 1].term
    end if
    logOk := (cLogTerm > lastTerm) or (cLogTerm = lastTerm and cLogLength >= log.length)
    if Term = currentTerm and logOk and votedFor = null then
        votedFor := cId
        send (VoteResponse, nodeld, currentTerm, true) to node cId
    else
        send (VoteResponse, nodeld, currentTerm, false) to node cId
    end if
end on
```
This phase handles the voting process when a candidate requests votes from other nodes.

* If the received term is greater than the current term, the node updates its term and role.
* The candidate's log is checked against the follower's log.
* If the follower has not voted in this term, and the candidate's log is at least as up-to-date as the follower's log, the follower votes for the candidate. Otherwise, it denies the vote.

## Collecting Votes

```shell
on receiving (voteResponse, voterId, term, granted) at nodeld do
    if currentRole = candidate and term = currentTerm and granted then
        votesReceived := votesReceived U {voterId}
        if votesReceived > (|nodes| + 1) / 2 then
            currentRole := leader
            currentLeader := nodeld
            cancel election timer
            for each follower in nodes \ {nodeld} do
                sentLength[follower] := log.length
                ackedLength[follower] := 0
                REPLICATELOG(nodeld, follower)
            end for
        end if
    else if term > currentTerm then
        currentTerm := term
        currentRole := follower
        votedFor := null
        cancel election timer
    end if
end on

```

This phase handles the response to a vote request from a candidate.

* If the node is a candidate and receives a vote response for the current term, it adds the vote to the received votes.
* If the candidate receives votes from a majority of the nodes, it becomes the leader.
* If the received term is greater than the current term, the node updates its term and role.

## Broadcasting Messages
```shell
on request to broadcast msg at node nodeld do
    if currentRole = leader then
        append the record (msg: msg, term: currentTerm) to log
        ackedLength[nodeld] := log.length
        for each follower in nodes \ {nodeld} do
            REPLICATELOG(nodeld, follower)
        end for
    else
        forward the request to currentLeader via a FIFO link
    end if
end on

periodically at node nodeld do
    if currentRole = leader then
        for each follower in nodes \ {nodeld} do
            REPLICATELOG(nodeld, follower)
        end for
    end if
end on

```

This phase handles broadcasting messages in the cluster.

* If the node is the leader, it appends the message to its log and replicates it to the followers.
* If the node is not the leader, it forwards the request to the current leader.

## Replicating from Leader to Followers

```shell
function REPLICATELOG(leaderId, followerId)
    prefixLen := sentLength[followerId]
    suffix := (log[prefixLen+1:], ...)
    prefixTerm := 0
    if prefixLen > 0 then
        prefixTerm := log[prefixLen].term
    end if
    send (LogRequest, leaderId, currentTerm, prefixLen, prefixTerm, commitLength, suffix) to followerId
end function

```
This phase handles log replication from the leader to its followers.

* The `REPLICATELOG` function iscalled when there are new log entries or periodically.
* The function determines the prefix and suffix of the log entries to send to a follower.
* A `LogRequest` message is sent to the follower containing the necessary log information.

## Followers Receiving Messages

```shell
on receiving (LogRequest, leaderId, term, prefixLen, prefixTerm, leaderCommit, suffix) at node nodeld do
    if term > currentTerm then
        currentTerm := term
        currentRole := follower
        currentLeader := leaderId
    end if
    logOk := (log.length >= prefixLen) and (prefixLen = 0 or log[prefixLen].term = prefixTerm)
    if term = currentTerm and logOk then
        APPENDENTRIES(prefixLen, leaderCommit, suffix)
        ack := prefixLen + suffix.length
        send (LogResponse, nodeld, currentTerm, ack, true) to leaderId
    else
        send (LogResponse, nodeld, currentTerm, 0, false) to leaderId
    end if
end on

```

This phase handles the log replication process from the leader to its followers.

* If the received term is greater than the current term, the node updates its term and role.
* The follower checks the consistency of the received log with its own log.
* If the logs are consistent, the follower appends the new entries and responds with an acknowledgment.
* If the logs are inconsistent, the follower responds with a negative acknowledgment.

## Updating Followers' Logs

```shell
function APPENDENTRIES(prefixLen, leaderCommit, suffix)
    if suffix.length > 0 and log.length >= prefixLen then
        index := min(log.length, prefixLen + suffix.length) - 1
        if log[index].term != suffix[index - prefixLen].term then
            log := log[1:prefixLen]
        end if
    end if
    if prefixLen + suffix.length > log.length then
        for i := log.length - prefixLen + 1 to suffix.length do
            log := log + suffix[i]
        end for
    end if
    if leaderCommit > commitLength then
        for i := commitLength + 1 to leaderCommit do
            deliver log[i].msg to the application
        end for
        commitLength := leaderCommit
    end if
end function

```

This function is called by followers to append log entries received from the leader.

* The function ensures that the log entries are appended correctly based on the received prefix and suffix.
* If any log entries conflict with existing entries, the conflicting entries are removed.
* The function updates the commit length and delivers committed entries to the application.

## Leader Receiving Log Acknowledgements

```shell
on receiving (LogResponse, follower, term, ack, success) at nodeld do
    if term = currentTerm and currentRole = leader then
        if success and ack > ackedLength[follower] then
            sentLength[follower] := ack
            ackedLength[follower] := ack
            COMMITLOGENTRIES()
        else if sentLength[follower] > 0 then
            sentLength[follower] := sentLength[follower] - 1
            REPLICATELOG(nodeld, follower)
        end if
    else if term > currentTerm then
        currentTerm := term
        currentRole := follower
        votedFor := null
        cancel election timer
    end if
end on

```

This phase handles the leader's receipt of acknowledgments from followers regarding log replication.

* If the received term is equal to the current term and the node is still the leader, it processes the acknowledgment.
* If the acknowledgment is successful and the acknowledgment index is greater than the previous acknowledgment index, the leader updates the replication progress.
* If the acknowledgment is not successful, the leader adjusts the replication progress accordingly.
* If the received term is greater than the current term, the node updates its term and role.

## Leader Committing Log Entries

```shell
function COMMITLOGENTRIES
	while commitLength < log.length do
		acks := 0
		for each node in nodes do
			if ackedLength[node] > commitLength then
				acks := acks + 1
			end if
		end for
		if acks > (|nodes| + 1) / 2 then
			deliver log[commitLength].msg to the application
			commitLength := commitLength + 1
		else
			break
		end if
	end while
end function

```

Any log entries that have been acknowledged by a quorum of nodes are ready to be committed by the leader. When a log entry is committed, its message is delivered to the application.

This README provides an overview of the Raft consensus algorithm, including the algorithm description and the corresponding pseudo code for each phase. For a complete understanding of the algorithm, it is recommended to refer to the original Raft paper (https://raft.github.io/raft.pdf).
