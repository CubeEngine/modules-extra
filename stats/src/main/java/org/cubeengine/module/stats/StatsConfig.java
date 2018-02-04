package org.cubeengine.module.stats;

import org.cubeengine.reflect.codec.yaml.ReflectedYaml;

@SuppressWarnings("all")
public class StatsConfig extends ReflectedYaml
{
    public String url = "http://localhost:8086";
    public String user = "user";
    public String password = "password";
    public String database = "cubeengine";
}
