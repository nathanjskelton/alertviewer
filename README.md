
## Bugle

```golang
tempNotification := Notification{

		AlertName: a.Labels["alertname"],
		Env:       a.Labels["env"],
		Team:      a.Labels["team"],
		Service:   service,
		Host:      host,
		State:     a.Labels["severity"],
		Info:      a.Annotations["summary"],
}

err := validateNotification(&tempNotification)
if err != nil {
		err_message := fmt.Errorf("alert %v, failed validation: %v", a.Labels["alertname"], err)
		return nil, err_message
}
return &tempNotification, nil
}
...
func (n *Notification) String() string {
	return fmt.Sprintf("%s: { \"system\": \"%s\", \"service\": \"%s\", \"host\": \"%s\", \"state\": \"%s\", \"info\": \"%s\" }",
	n.Team, n.Env, n.Service, n.Host, n.State, n.Info)
}
```

- [x] Incoming: add system, favor system over env (still support env but use system if provided). still send to eventviewer as system: like it is today
- [x] add configurabe ability to use tcp or udp

## AlertManager
```json
  {
    "annotations": {
      "service": "Prom_check",
      "summary": "Alert with critical severity"
    },
    "endsAt": "2023-08-10T19:21:39.727Z",
    "fingerprint": "e046ff5c9ac8ed21",
    "receivers": [
      {
        "name": "bugle"
      }
    ],
    "startsAt": "2023-08-10T19:17:39.727Z",
    "status": {
      "inhibitedBy": [],
      "silencedBy": [],
      "state": "active"
    },
    "updatedAt": "2023-08-10T19:17:39.830Z",
    "generatorURL": "http://ee0f539bd0f1:9090/graph?g0.expr=up%7Bjob%3D%22services%22%7D+%3C+1&g0.tab=1",
    "labels": {
      "alertname": "ValidAlertCriticalNoInstance",
      "env": "devbox",
      "instance": "idonotexist:564",
      "job": "services",
      "severity": "critical"
    }
  }
```

## Model

### LogEntry
- String id //fingerprint

- List of **Note** notes
- Indexed LogEntryStatus status
	- Enum: NEW, TRIAGE, WATCH, HIDE
- boolean regex
- Alert alertDocument

#### remove
- Indexed List of LogEntryTeam teams
	- Enum: LIVE, OPS, MISSION, DEV
- LocalDateTime lastChange
- String lastUser
- List of Occurence occurences
- int totalOccurences
- Indexed String logType
	- critical, warning, info, trace
- Indexed LocalDateTime startTime
- Indexed LocalDateTime endTime
- Indexed String message

### Occurrence
> An occurence is a 

- LocalDateTime time
- String id


### Note
- LocalDateTime timestamp
- String user
- String message


# Silence

```json
{
  "status": "success",
  "data": [
    {
      "id": "eced0fd0-198a-4710-a492-a903f85dfcf0",
      "matchers": [
        {
          "name": "alername1",
          "value": "mytest.*",
          "isRegex": true,
          "isEqual": true
        }
      ],
      "startsAt": "2023-09-21T20:33:56.672846144Z",
      "endsAt": "2023-09-23T00:00:00Z",
      "updatedAt": "2023-09-21T20:33:56.672846144Z",
      "createdBy": "api",
      "comment": "Silence",
      "status": {
        "state": "active"
      }
    }
  ]
}
```

