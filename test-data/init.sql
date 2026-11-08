-- Docker Proxy Rule test database initialization script

-- Create test tables for proxy connections
CREATE TABLE IF NOT EXISTS proxy_connections (
    id SERIAL PRIMARY KEY,
    client_ip INET NOT NULL,
    target_host VARCHAR(255) NOT NULL,
    target_port INTEGER NOT NULL,
    connection_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    bytes_transferred BIGINT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'active'
);

-- Create test configuration table
CREATE TABLE IF NOT EXISTS proxy_config (
    id SERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial configuration data
INSERT INTO proxy_config (config_key, config_value) VALUES
    ('proxy_port', '1080'),
    ('max_connections', '100'),
    ('timeout_seconds', '300'),
    ('log_level', 'INFO')
ON CONFLICT (config_key) DO NOTHING;

-- Insert test sample data
INSERT INTO proxy_connections (client_ip, target_host, target_port, bytes_transferred, status) VALUES
    ('172.20.0.1', 'test-web-server', 80, 1024, 'completed'),
    ('172.20.0.2', 'test-redis', 6379, 512, 'completed'),
    ('172.20.0.3', 'external-api.example.com', 443, 2048, 'active')
ON CONFLICT DO NOTHING;

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_proxy_connections_client_ip ON proxy_connections(client_ip);
CREATE INDEX IF NOT EXISTS idx_proxy_connections_connection_time ON proxy_connections(connection_time);
CREATE INDEX IF NOT EXISTS idx_proxy_connections_status ON proxy_connections(status);
