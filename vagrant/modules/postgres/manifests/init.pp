class postgres($server_ip) {
  package { 'postgresql':
    name      =>  'postgresql-13',
    ensure    =>  'installed',
  }

  file {'pg_hba.conf':
    path      =>  '/etc/postgresql/13/main/pg_hba.conf',
    ensure    =>  'file',
    owner     =>  'postgres',
    group     =>  'postgres',
    mode      =>  '0640',
    content   =>  epp('postgres/pg_hba.conf.epp', {'server_ip' => $server_ip}),
    require   =>  Package['postgresql'],
  }

  file {'server.conf':
    path      =>  '/etc/postgresql/13/main/conf.d/server.conf',
    ensure    =>  'file',
    owner     =>  'postgres',
    group     =>  'postgres',
    mode      =>  '0640',
    content   =>  epp('postgres/server.conf.epp', {'server_ip' => $server_ip}),
    require   =>  Package['postgresql']
  }

  service {'postgresql':
    name      => 'postgresql',
    ensure    =>  true,
    enable    =>  true,
    subscribe =>  [File['pg_hba.conf'], File['server.conf']],
  }

  file {'init-db':
    path      =>  '/usr/local/bin/init-db.sh',
    ensure    =>  'file',
    owner     =>  'postgres',
    group     =>  'postgres',
    mode      =>  '0700',
    content   =>  epp('postgres/init-db.sh.epp'),
    require   =>  Service['postgresql'],
  }

  exec {'init-db':
    command   =>  '/usr/local/bin/init-db.sh',
    user      =>  'postgres',
    require   =>  File['init-db'],
  }
}
