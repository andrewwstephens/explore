#!/bin/bash

gq https://user-prefs-master.herokuapp.com/v1/graphql --introspect >common-graphql/src/main/resources/graphql/schemas/UserPreferencesDB.graphql
