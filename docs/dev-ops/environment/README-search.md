# Elasticsearch 与 Canal 环境

本目录中的 `docker-compose.yml` 为项目提供以下服务。它们部署在
`192.168.94.128`，而 MySQL、Redis、RabbitMQ 和 Nacos 继续使用
`111.228.17.153`：

| 服务 | 版本 | 宿主机端口 |
| --- | --- | --- |
| Elasticsearch | 7.17.14 | 9200 |
| Kibana | 7.17.14 | 5601 |
| Canal Server | 1.1.7 | 11111 |
| Canal Adapter | 1.1.6 | 8082 |
| Portainer CE | 2.21.5 | 9000 |

Elasticsearch、Kibana 和 Canal 使用阿里云镜像仓库，以便在国内网络环境下拉取。Canal Adapter 使用 `es7` 插件，将 `big_market_01`、`big_market_02` 中的 `raffle_activity_order_000`～`003` 和 `user_raffle_order_000`～`003` 同步到 Elasticsearch。

## 启动

```bash
cd docs/dev-ops/environment
docker compose up -d elasticsearch kibana canal-server canal-adapter portainer
```

只停止本组服务：

```bash
docker compose stop elasticsearch kibana canal-server canal-adapter portainer
```

## 初始化索引

首次启动时，`elasticsearch-init` 会创建 `qijiv` 超级用户并设置 Kibana
服务账号。然后创建两个业务索引：

```bash
ELASTICSEARCH_USER=qijiv ELASTICSEARCH_PASSWORD=123456 sh elasticsearch/init-indexes.sh
```

如需将某张表的既有数据全量导入 ES，可调用 Adapter 的 ETL 接口：

```bash
curl -X POST "http://127.0.0.1:8082/etl/es7/big_market_01_raffle_activity_order_000.yml?params=1970-01-01%2000:00:00"
```

后续 MySQL binlog 增量由 Canal Server 与 Canal Adapter 自动消费。

## 检查

```bash
curl -u qijiv:123456 http://127.0.0.1:9200/_cluster/health
curl http://127.0.0.1:8082/destinations
curl -u qijiv:123456 'http://127.0.0.1:9200/_cat/indices/big_market.*?v'
docker compose ps elasticsearch kibana canal-server canal-adapter portainer
```

## 访问地址和账号

| 服务 | 地址 | 账号 | 密码 |
| --- | --- | --- | --- |
| Elasticsearch | `http://192.168.94.128:9200` | `qijiv` | `123456` |
| Kibana | `http://192.168.94.128:5601` | `qijiv` | `123456` |
| Portainer | `http://192.168.94.128:9000` | `qijiv` | `18903705503qQ` |

Canal Server 与 Adapter 的 TCP 通道使用 `qijiv / 123456`。Adapter 的 HTTP
管理接口不提供内置登录认证，请只在受信任内网开放 `8082`。

虚拟机上的部署目录为 `/home/qijiv/marketing-platform-environment`。
