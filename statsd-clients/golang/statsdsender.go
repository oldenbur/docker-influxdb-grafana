package main

import (
	"fmt"
	"gopkg.in/alexcesaro/statsd.v2"
	"os"
)

type StatsdSender interface {
	Count(field string, val interface{})
	Timing(field string, val interface{})
	Gauge(field string, val interface{})
	Close()
}

func NewStatsdSender(endpoint, clusterid, service string) (StatsdSender, error) {

	var err error
	var host string
	if host, err = os.Hostname(); err != nil {
		return nil, fmt.Errorf("hostname error: ", err)
	}

	return statsd.New(
		statsd.Address(endpoint),
		statsd.TagsFormat(statsd.InfluxDB),
		statsd.Tags("cluster", clusterid, "host", host, "service", service),
	)
}
