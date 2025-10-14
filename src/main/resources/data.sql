-- INSERTS PARA TB_CLIENTE (11 clientes - mantidos da versão anterior)
INSERT INTO TB_CLIENTE (NOME_CLIENTE, CPF) VALUES 
('JOÃO SILVA', '123.456.789-01'), ('MARIA OLIVEIRA', '987.654.321-09'), ('CARLOS SOUZA', '456.789.123-45'),
('ANA SANTOS', '321.654.987-09'), ('PEDRO COSTA', '789.123.456-78'), ('DAVI FRANCO', '531.762.574-32'),
('MARCUS SAMPAIO', '766.243.213-98'), ('LEANDRO TEDESCKE', '745.765.132-53'), ('BRUNA MACRUZ', '686.655.354-89'),
('GUSTAVO TADANO', '456.234.132-79'), ('ALISZOM FERNANDEZ', '632.987.432-23');

-- INSERTS PARA TB_EMAIL (para todos os 11 clientes - mantidos)
INSERT INTO TB_EMAIL (END_EMAIL, FK_CLIENTE) VALUES 
('joao.silva@gmail.com', 1), ('maria.oliveira@hotmail.com', 2), ('carlos.souza@yahoo.com', 3), 
('ana.santos@outlook.com', 4), ('pedro.costa@gmail.com', 5), ('davi.franco@example.com', 6),
('marcus.sampaio@example.com', 7), ('leandro.tedescke@example.com', 8), ('bruna.macruz@example.com', 9),
('gustavo.tadano@example.com', 10), ('aliszom.fernandez@example.com', 11);

-- INSERTS PARA TB_ENDERECO (para todos os 11 clientes - mantidos)
INSERT INTO TB_ENDERECO (RUA, CEP, BAIRRO, CIDADE, NUMERO_CASA, FK_CLIENTE) VALUES 
('Rua das Flores', '01234-567', 'Centro', 'São Paulo', 100, 1), ('Avenida Brasil', '98765-432', 'Jardins', 'Rio de Janeiro', 200, 2),
('Rua das Palmeiras', '45678-901', 'Vila Nova', 'Belo Horizonte', 300, 3), ('Alameda Santos', '23456-789', 'Paraíso', 'São Paulo', 400, 4),
('Rua do Comércio', '34567-890', 'Centro', 'Curitiba', 500, 5), ('Rua Augusta', '01305-000', 'Consolação', 'São Paulo', 601, 6),
('Avenida Paulista', '01311-000', 'Bela Vista', 'São Paulo', 702, 7), ('Rua da Bahia', '30160-010', 'Lourdes', 'Belo Horizonte', 803, 8),
('Avenida Atlântica', '22070-000', 'Copacabana', 'Rio de Janeiro', 904, 9), ('Rua Oscar Freire', '01426-000', 'Jardim Paulista', 'São Paulo', 1005, 10),
('Praça da Sé', '01001-000', 'Sé', 'São Paulo', 1106, 11);

-- INSERTS PARA TB_TELEFONE (para todos os 11 clientes - mantidos)
INSERT INTO TB_TELEFONE (TELEFONE, FK_CLIENTE) VALUES 
('(11) 99999-1111', 1), ('(21) 98888-2222', 2), ('(31) 97777-3333', 3), ('(11) 96666-4444', 4),
('(41) 95555-5555', 5), ('(11) 94444-6666', 6), ('(11) 93333-7777', 7), ('(31) 92222-8888', 8),
('(21) 91111-9999', 9), ('(11) 99000-1010', 10), ('(11) 98080-2020', 11);

-- INSERTS PARA TB_CATEGORIA (mantidos da versão anterior)
INSERT INTO TB_CATEGORIA (NOME) VALUES 
('Smartphones'), ('Notebooks & PCs'), ('Tablets'), ('Acessórios Diversos'),
('Componentes de Reposição'), ('Periféricos'), ('Smartwatches & Wearables'), ('Consoles & Jogos');

-- INSERTS PARA TB_MARCA (mantidos da versão anterior)
INSERT INTO TB_MARCA (NOME) VALUES 
('Samsung'), ('Apple'), ('Xiaomi'), ('Motorola'), ('Dell'), ('HP'), ('Lenovo'), ('Asus'),
('Sony'), ('Microsoft'), ('Nintendo'), ('JBL'), ('Logitech'), ('Kingston'), ('Intel');

-- INSERTS PARA TB_PRODUTO (15 produtos - mantidos da versão anterior)
INSERT INTO TB_PRODUTO (NOME, CUSTO, VALOR, QUANTIDADE, DESCRICAO, FK_CATEGORIA, FK_MARCA) VALUES 
('Smartphone Galaxy S23', 3500.00, 4800.00, 15, 'Smartphone avançado com câmera tripla.', 1, 1),
('iPhone 15 Pro', 6500.00, 8500.00, 10, 'Último lançamento da Apple com chip A17.', 1, 2),
('Notebook Dell Inspiron 15', 2800.00, 3900.00, 20, 'Notebook versátil para trabalho e estudo.', 2, 5),
('SSD Kingston NV2 1TB', 250.00, 450.00, 50, 'SSD NVMe PCIe 4.0 de alta velocidade.', 5, 14),
('Cabo USB-C Reforçado 2m', 30.00, 70.00, 100, 'Cabo trançado para maior durabilidade.', 4, 1),
('Tela Display iPhone 12 Original', 450.00, 750.00, 25, 'Peça de reposição original para iPhone 12.', 5, 2),
('Bateria Samsung Galaxy A54', 90.00, 180.00, 40, 'Bateria de reposição para Galaxy A54.', 5, 1),
('Mouse Gamer Logitech G502', 220.00, 350.00, 30, 'Mouse com sensor HERO e pesos ajustáveis.', 6, 13),
('Fone de Ouvido JBL Wave Buds', 180.00, 280.00, 60, 'Fone TWS com Deep Bass Sound.', 4, 12),
('PlayStation 5 Digital Edition', 3200.00, 4000.00, 8, 'Console de última geração, versão digital.', 8, 9),
('Teclado Mecânico HyperX Alloy Origins', 400.00, 650.00, 12, 'Teclado compacto com switches HyperX Red.', 6, 14), 
('Carregador Rápido 45W Samsung Trio', 150.00, 250.00, 35, 'Carregador com 3 portas e Super Fast Charging.', 4, 1),
('Tablet Xiaomi Pad 6', 1800.00, 2500.00, 18, 'Tablet com tela de 11" 144Hz.', 3, 3),
('Smartwatch Amazfit GTS 4 Mini', 450.00, 700.00, 22, 'Smartwatch leve com GPS e longa bateria.', 7, 3), 
('Memória RAM DDR4 8GB 3200MHz Kingston Fury', 130.00, 220.00, 45, 'Módulo de memória para upgrade de PC/Notebook.', 5, 14);

-- INSERTS PARA TB_ESTOQUE (espelhando TB_PRODUTO.QUANTIDADE - mantidos)
INSERT INTO TB_ESTOQUE (QUANTIDADE, FK_PRODUTO) VALUES 
(15, 1), (10, 2), (20, 3), (50, 4), (100, 5), (25, 6), (40, 7), (30, 8), (60, 9), (8, 10),
(12, 11), (35, 12), (18, 13), (22, 14), (45, 15);

-- INSERTS PARA TB_SERVICO (10 serviços - mantidos da versão anterior)
INSERT INTO TB_SERVICO (NOME, DESCRICAO_SERVICO, VALOR) VALUES 
('Troca de Tela Smartphone Padrão', 'Substituição de tela LCD/OLED danificada.', 350.00),
('Troca de Tela Tablet', 'Substituição de tela de tablet danificada.', 450.00),
('Troca de Bateria (Smartphone/Tablet)', 'Substituição de bateria viciada ou defeituosa.', 150.00),
('Reparo Conector de Carga', 'Conserto ou substituição do conector de carregamento.', 120.00),
('Formatação e Instalação de S.O. (PC/Notebook)', 'Instalação limpa do sistema operacional e drivers.', 180.00),
('Limpeza Interna e Troca Pasta Térmica', 'Limpeza de poeira e aplicação de nova pasta térmica.', 150.00),
('Diagnóstico Técnico Avançado', 'Análise completa para identificação de defeitos complexos.', 80.00),
('Backup Completo de Dados (até 500GB)', 'Cópia de segurança de arquivos do usuário.', 90.00),
('Reparo de Placa-mãe Smartphone (Nível Básico)', 'Reparos mais simples em placa lógica.', 250.00),
('Instalação de Software Específico', 'Instalação e configuração de softwares solicitados.', 70.00);

-- INSERTS PARA TB_VENDA (Total: 30 vendas, todas na última semana de Maio/Início de Junho 2025)
-- LUCRO = (PRODUTO.VALOR - PRODUTO.CUSTO) * QUANTIDADE
-- VALOR_VENDA = PRODUTO.VALOR * QUANTIDADE
INSERT INTO TB_VENDA (QUANTIDADE, DATA_INICIO, HORA_INICIO, VALOR, LUCRO, FK_PRODUTO, FK_CLIENTE) VALUES 
(1, '2025-05-29', '10:30:00', 4800.00, 1300.00, 1, 1), (1, '2025-05-29', '11:15:00', 350.00, 130.00, 8, 2),
(2, '2025-05-29', '14:00:00', 140.00, 80.00, 5, 3),   (1, '2025-05-29', '16:45:00', 8500.00, 2000.00, 2, 4),
(1, '2025-05-30', '09:20:00', 280.00, 100.00, 9, 5),  (1, '2025-05-30', '10:55:00', 450.00, 200.00, 4, 6),
(1, '2025-05-30', '13:10:00', 3900.00, 1100.00, 3, 7), (1, '2025-05-30', '15:00:00', 250.00, 100.00, 12, 8),
(2, '2025-05-31', '11:30:00', 440.00, 180.00, 15, 9), (1, '2025-05-31', '12:00:00', 2500.00, 700.00, 13, 10),
(1, '2025-05-31', '14:25:00', 700.00, 250.00, 14, 11),(1, '2025-05-31', '16:00:00', 4000.00, 800.00, 10, 1),
(1, '2025-06-01', '09:10:00', 750.00, 300.00, 6, 3),  (1, '2025-06-01', '10:40:00', 180.00, 90.00, 7, 4),
(1, '2025-06-01', '11:20:00', 650.00, 250.00, 11, 5), (1, '2025-06-01', '15:00:00', 4800.00, 1300.00, 1, 6),
(2, '2025-06-02', '09:00:00', 140.00, 80.00, 5, 7),   (1, '2025-06-02', '10:00:00', 8500.00, 2000.00, 2, 8),
(1, '2025-06-02', '14:30:00', 350.00, 130.00, 8, 9),  (1, '2025-06-02', '17:00:00', 450.00, 200.00, 4, 10),
(1, '2025-06-03', '09:05:00', 2500.00, 700.00, 13, 11),(1, '2025-06-03', '11:45:00', 220.00, 90.00, 15, 1),
(1, '2025-06-03', '15:15:00', 70.00, 40.00, 5, 2),    (1, '2025-06-03', '16:00:00', 4000.00, 800.00, 10, 3),
(1, '2025-06-04', '09:00:00', 4800.00, 1300.00, 1, 4), (1, '2025-06-04', '10:00:00', 280.00, 100.00, 9, 5),
(1, '2025-06-04', '11:00:00', 650.00, 250.00, 11, 6), (1, '2025-06-04', '12:00:00', 700.00, 250.00, 14, 7),
(1, '2025-06-04', '14:00:00', 3900.00, 1100.00, 3, 8), (1, '2025-06-04', '15:30:00', 250.00, 100.00, 12, 9);

-- INSERTS PARA TB_OS (Total: 30 Ordens de Serviço, todas na última semana de Maio/Início de Junho 2025)
-- VALOR_OS = SERVICO.VALOR + (PRODUTO.VALOR * OS.QUANTIDADE)
-- LUCRO_OS = SERVICO.VALOR + ((PRODUTO.VALOR - PRODUTO.CUSTO) * OS.QUANTIDADE)
-- Para OS sem consumo de produto da loja (QTD=0), VALOR_OS e LUCRO_OS são apenas SERVICO.VALOR
INSERT INTO TB_OS (DATA_INICIO, HORA_INICIO, DATA_FIM, HORA_FIM, QUANTIDADE, VALOR, LUCRO, STATUS_OS, FK_SERVICO, FK_PRODUTO, FK_CLIENTE) VALUES 
('2025-05-29', '09:00:00', '2025-05-29', '12:00:00', 1, 1100.00, 650.00, 'Concluido', 1, 6, 1),
('2025-05-29', '10:00:00', '2025-05-29', '11:30:00', 1, 330.00, 240.00, 'Concluido', 3, 7, 2),
('2025-05-29', '14:00:00', NULL, NULL, 1, 80.00, 80.00, 'Registrada', 7, 1, 3), -- Valor e Lucro apenas do serviço diagnóstico
('2025-05-29', '11:00:00', '2025-05-30', '17:00:00', 0, 180.00, 180.00, 'Concluido', 5, 3, 4), -- QTD=0 para formatação
('2025-05-30', '13:00:00', NULL, NULL, 1, 150.00, 150.00, 'Em Andamento', 6, 10, 5),
('2025-05-30', '09:30:00', '2025-05-30', '10:00:00', 0, 90.00, 90.00, 'Cancelado', 8, 4, 6),
('2025-05-30', '16:00:00', NULL, NULL, 1, 120.00, 120.00, 'Registrada', 4, 2, 7),
('2025-05-31', '10:45:00', '2025-05-31', '18:00:00', 0, 150.00, 150.00, 'Concluido', 6, 11, 8),
('2025-05-31', '14:15:00', NULL, NULL, 1, 250.00, 250.00, 'Em Andamento', 9, 13, 9),
('2025-05-31', '11:50:00', '2025-06-01', '12:30:00', 0, 70.00, 70.00, 'Cliente Ausente', 10, 12, 10),
('2025-06-01', '15:00:00', NULL, NULL, 1, 450.00, 450.00, 'Registrada', 2, 13, 11),
('2025-06-01', '09:15:00', '2025-06-01', '17:45:00', 1, 400.00, 340.00, 'Concluido', 3, 14, 1),
('2025-06-01', '14:00:00', '2025-06-02', '10:00:00', 1, 600.00, 470.00, 'Concluido', 1, 1, 2), -- Troca Tela S23
('2025-06-02', '09:30:00', NULL, NULL, 1, 150.00, 150.00, 'Em Andamento', 3, 2, 3), -- Troca Bateria iPhone
('2025-06-02', '11:00:00', '2025-06-03', '14:00:00', 0, 80.00, 80.00, 'Concluido', 7, 5, 4), -- Diagnóstico (Produto 5 simbólico)
('2025-06-02', '15:00:00', '2025-06-02', '16:00:00', 1, 270.00, 220.00, 'Concluido', 4, 4, 5), -- Reparo Conector + SSD (exemplo, SSD não é conector)
('2025-06-02', '17:00:00', NULL, NULL, 0, 180.00, 180.00, 'Registrada', 5, 3, 6),
('2025-06-03', '09:00:00', '2025-06-04', '10:30:00', 1, 370.00, 280.00, 'Concluido', 6, 7, 7), -- Limpeza + Bateria A54
('2025-06-03', '10:15:00', NULL, NULL, 0, 90.00, 90.00, 'Em Andamento', 8, 8, 8),
('2025-06-03', '13:30:00', '2025-06-03', '15:00:00', 1, 250.00, 250.00, 'Cancelado', 9, 1, 9), -- Reparo Placa S23 (Cancelado)
('2025-06-03', '16:00:00', NULL, NULL, 0, 70.00, 70.00, 'Registrada', 10, 9, 10),
('2025-06-03', '17:20:00', '2025-06-04', '11:00:00', 1, 800.00, 570.00, 'Concluido', 2, 13, 11), -- Troca Tela Tablet Xiaomi
('2025-06-04', '09:00:00', NULL, NULL, 1, 350.00, 350.00, 'Registrada', 1, 1, 1),
('2025-06-04', '09:45:00', NULL, NULL, 1, 120.00, 120.00, 'Em Andamento', 4, 14, 2),
('2025-06-04', '10:30:00', '2025-06-04', '12:30:00', 0, 180.00, 180.00, 'Concluido', 5, 15, 3),
('2025-06-04', '11:15:00', NULL, NULL, 1, 150.00, 150.00, 'Registrada', 6, 4, 4),
('2025-06-04', '14:00:00', '2025-06-04', '14:30:00', 0, 80.00, 80.00, 'Cancelado', 7, 5, 5),
('2025-06-04', '15:00:00', NULL, NULL, 0, 90.00, 90.00, 'Em Andamento', 8, 6, 6),
('2025-06-04', '16:00:00', '2025-06-04', '17:00:00', 1, 250.00, 250.00, 'Concluido', 9, 7, 7),
('2025-06-04', '17:30:00', NULL, NULL, 0, 70.00, 70.00, 'Registrada', 10, 8, 8);

INSERT INTO TB_FUNCIONARIO (NOME_FUN, CPF_FUN, DATA_ADM, STATUS_FUN, NIVEL_ACESS) VALUES
('Ana Silva', '111.222.333-44', '2022-08-15', 'Ativo', 'administrador'),
('Bruno Costa', '222.333.444-55', '2023-01-20', 'Ativo', 'funcionario'),
('Carla Dias', '333.444.555-66', '2023-05-10', 'Ativo', 'funcionario'),
('Daniel Souza', '444.555.666-77', '2023-09-01', 'Ativo', 'funcionario');