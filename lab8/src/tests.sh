#!/bin/bash

# 1. Limpeza inicial (mata processos antigos e limpa pastas)
echo "--- Limpando ambiente ---"
pkill -f "main.go" # Mata qualquer go rodando
rm -rf data_client1 data_client2
mkdir -p data_client1
mkdir -p data_client2

# Cria arquivos iniciais para ter hash
echo "Conteudo A" > data_client1/fileA.txt
echo "Conteudo B" > data_client2/fileB.txt

# 2. Inicia o Servidor com Race Detector
echo "--- Iniciando Servidor ---"
cd server
go run main.go > server.log 2>&1 &
SERVER_PID=$!
cd ..
sleep 2 # Espera o servidor subir

# 3. Prepara os inputs automáticos para os clientes
# O cliente espera: IP -> Loop (Opção -> Hash -> ...)
# Vamos fazer o Cliente 1 ficar consultando (Query) loucamente
# Assumindo que o hash do 'fileB.txt' ("Conteudo B") seja calculado (ex: 999 - fictício)
# Aqui geramos um input que repete a opção 1 varias vezes

echo "127.0.0.1" > input_c1.txt
for i in {1..50}; do
    echo "1" >> input_c1.txt # Opção Query
    echo "123" >> input_c1.txt # Hash qualquer (só pra testar o lock)
done
echo "3" >> input_c1.txt # Exit no final

# O Cliente 2 vai apenas ficar parado monitorando (o input é só o IP)
echo "127.0.0.1" > input_c2.txt
# Mantém ele rodando sem escolher opção por um tempo
sleep 10 >> input_c2.txt 

# 4. Inicia Clientes em Background
echo "--- Iniciando Clientes ---"
# Cliente 1 (Porta 9091, Peer 9092)
go run client/main.go 9091 9092 ./data_client1 < input_c1.txt > client1.log 2>&1 &
C1_PID=$!

# Cliente 2 (Porta 9092, Peer 9091)
go run client/main.go 9092 9091 ./data_client2 < input_c2.txt > client2.log 2>&1 &
C2_PID=$!

echo "--- Teste de Estresse Rodando ---"
echo "O Cliente 1 está fazendo 50 queries seguidas..."
echo "O Script vai criar e deletar arquivos no Cliente 2 simultaneamente..."

# 5. CAOS: Cria e deleta arquivos no Cliente 2 para disparar o Monitor (fsnotify)
# Isso força o 'updateServer' a rodar ao mesmo tempo que as 'queryHash'
for i in {1..20}; do
    touch data_client2/temp_$i.txt
    sleep 0.1
    rm data_client2/temp_$i.txt
done

# 6. Espera e Limpeza
wait $C1_PID
echo "Cliente 1 terminou."

kill $SERVER_PID
kill $C2_PID

echo "--- Teste Finalizado ---"
echo "Verifique os arquivos de log (server.log, client1.log, client2.log)"
echo "Procure por 'WARNING: DATA RACE' ou 'panic'."