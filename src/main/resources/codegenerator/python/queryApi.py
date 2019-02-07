#   Copyright (c) 2018, EPFL/Human Brain Project PCO
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
import json
import os
from abc import abstractmethod
from typing import TypeVar, Generic, Sequence

from openid_http_client.auth_client.access_token_client import AccessTokenClient
from openid_http_client.auth_client.auth_client import AbstractAuthClient
from openid_http_client.auth_client.simple_refresh_token_client import SimpleRefreshTokenClient
from openid_http_client.http_client import HttpClient


T = TypeVar('T')

class KGClient:

    def __init__(self, auth_client: AbstractAuthClient, endpoint: str):
        self.http_client = HttpClient(endpoint, "", auth_client=auth_client)

    def query(self, root_schema, query_name, size, start, filter_parameters):
        url = "{}/{}/instances?size={}&start={}{}".format(root_schema, query_name, size if size is not None else "", start if start is not None else "", filter_parameters if filter_parameters is not None else "")
        return self.http_client.get(url)

    @staticmethod
    def _get_configuration():
        with open(os.path.join(os.path.dirname(__file__), "configuration.json")) as global_config_file:
            config = json.load(global_config_file)
        return config

    @staticmethod
    def by_single_token(access_token: str, endpoint: str):
        auth_client = AccessTokenClient(access_token)
        return KGClient(auth_client, endpoint)

    @staticmethod
    def by_single_token_from_config(access_token:str):
        return KGClient.by_single_token(access_token, KGClient._get_configuration()["endpoint"])

    @staticmethod
    def by_refresh_token(openid_host, client_secret, client_id, refresh_token, endpoint: str):
        auth_client = SimpleRefreshTokenClient(openid_host, client_secret, client_id, refresh_token)
        return KGClient(auth_client, endpoint)

    @staticmethod
    def by_refresh_token_from_config():
        oidc_config = KGClient._get_oidc_configuration()
        return KGClient.by_refresh_token(oidc_config["openid_host"], oidc_config["client_secret"], oidc_config["client_id"], oidc_config["refresh_token"], KGClient._get_configuration()["endpoint"])

    @staticmethod
    def _get_oidc_configuration():
        with open(KGClient._get_configuration()["oidc_config"]) as oidc_conf_file:
            oidc_config = json.load(oidc_conf_file)
        return oidc_config


class Query(Generic[T]):
    def __init__(self, client:KGClient, root_schema:str, query_name:str):
        self._client = client
        self._root_schema = root_schema
        self._query_name = query_name
        self._last_size = None
        self._last_start = None
        self._last_count = None

    @abstractmethod
    def create_result(self, payload) -> T:
        return NotImplemented

    @abstractmethod
    def create_filter_params(self) -> str:
        return NotImplemented

    def fetch(self, size=None, start=0) -> Sequence[T]:
        results = self._client.query(self._root_schema, self._query_name, size, start, self.create_filter_params())
        return self._handle_results(results, size, start)

    def _handle_results(self, results, size, start) -> Sequence[T]:
        self._last_size = size
        self._last_start = start
        self._last_count = results["total"]
        return [self.create_result(r) for r in results["results"]]

    def next_page(self) -> Sequence[T]:
        if self.has_more_items()!=None:
            results = self._client.query(self._root_schema, self._query_name, self._last_size, self._last_start, self.create_filter_params())
            return self._handle_results(results, self._last_size, self._last_size+self._last_start)
        return []

    def has_more_items(self):
        if self._last_count is None or self._last_count>self._last_size+self._last_start:
            return True
        return False
