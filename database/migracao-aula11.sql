-- Migracao para a aula 11: as tabelas da aula de Model passam a ter os nomes que o banco.py usa.
--
-- Rode UMA vez no banco que ja tem as tabelas antigas (clientes, contratos, consultores, servicos),
-- ANTES de subir o backend novo. Os dados sao mantidos: usuarios continuam ligados aos consultores.
-- Banco novo, sem essas tabelas, nao precisa: o Spring cria tudo.
--
-- No DBeaver: SQL Editor > cole o arquivo inteiro > Alt+X (executar script).

DO $$
DECLARE
	r record;
BEGIN
	IF to_regclass('public.clientes') IS NULL THEN
		RAISE NOTICE 'Nada a migrar: a tabela clientes nao existe.';
		RETURN;
	END IF;
	IF to_regclass('public.cliente') IS NOT NULL THEN
		RAISE EXCEPTION 'A tabela cliente ja existe: o backend novo subiu antes da migracao. Apague cliente, contrato, consultor e servico (vazias) e rode de novo.';
	END IF;

	ALTER TABLE consultores RENAME TO consultor;
	ALTER TABLE servicos RENAME TO servico;
	ALTER TABLE servico ALTER COLUMN nome TYPE varchar(120);

	ALTER TABLE clientes RENAME TO cliente;
	ALTER TABLE cliente RENAME COLUMN codigo TO codigo_cti;
	ALTER TABLE cliente ALTER COLUMN codigo_cti TYPE varchar(60);
	ALTER TABLE cliente ALTER COLUMN nome TYPE varchar(255);
	ALTER TABLE cliente ALTER COLUMN cidade TYPE varchar(120);
	ALTER TABLE cliente ALTER COLUMN cidade DROP NOT NULL;
	ALTER TABLE cliente ALTER COLUMN uf DROP NOT NULL;
	-- O codigo deixa de ser unico: cada lote grava os seus clientes.
	FOR r IN SELECT conname FROM pg_constraint WHERE conrelid = 'cliente'::regclass AND contype = 'u' LOOP
		EXECUTE format('ALTER TABLE cliente DROP CONSTRAINT %I', r.conname);
	END LOOP;

	ALTER TABLE contratos RENAME TO contrato;
	ALTER TABLE contrato RENAME COLUMN data_inicio TO data_contratacao;
	-- Apagar o cliente apaga os contratos (o banco.py reprocessa o lote assim).
	FOR r IN SELECT conname FROM pg_constraint
			WHERE conrelid = 'contrato'::regclass AND contype = 'f' AND confrelid = 'cliente'::regclass LOOP
		EXECUTE format('ALTER TABLE contrato DROP CONSTRAINT %I', r.conname);
	END LOOP;
	ALTER TABLE contrato ADD CONSTRAINT fk_contrato_cliente
		FOREIGN KEY (cliente_id) REFERENCES cliente ON DELETE CASCADE;

	RAISE NOTICE 'Migracao concluida. Suba o backend: ele cria upload_lote, upload_linha e estatistica_lote.';
END $$;
