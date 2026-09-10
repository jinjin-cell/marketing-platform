#!/usr/bin/env bash
set -e

export DEBIAN_FRONTEND=noninteractive
export NEEDRESTART_MODE=a

echo "==> [1/6] apt-get update"
apt-get update

echo "==> [2/6] install prerequisites (ca-certificates curl gnupg)"
apt-get install -y ca-certificates curl gnupg

echo "==> [3/6] add Docker GPG key"
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor --yes -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo "==> [4/6] add Docker apt repository"
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

echo "==> [5/6] apt-get update (with Docker repo)"
apt-get update

echo "==> [6/6] install docker-ce + tools"
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "==> add user qijiv to docker group"
usermod -aG docker qijiv || true

echo "==> ensure docker service enabled"
systemctl enable --now docker || true

echo "==> DONE"
docker --version
docker compose version
echo "OK"
