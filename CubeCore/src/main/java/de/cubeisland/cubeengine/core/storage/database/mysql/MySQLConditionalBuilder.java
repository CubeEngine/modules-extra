package de.cubeisland.cubeengine.core.storage.database.mysql;

import de.cubeisland.cubeengine.core.storage.database.ConditionalBuilder;
import de.cubeisland.cubeengine.core.storage.database.FunctionBuilder;
import de.cubeisland.cubeengine.core.util.Validate;

/**
 *
 * @author Anselm Brehme
 */
public abstract class MySQLConditionalBuilder<T extends ConditionalBuilder> extends MySQLBuilderBase implements ConditionalBuilder<T>
{
    private MySQLFunctionBuilder functionBuilder;

    protected MySQLConditionalBuilder(MySQLQueryBuilder builder)
    {
        super(builder);
        this.functionBuilder = null;
    }

    public MySQLFunctionBuilder beginFunction(String function)
    {
        this.beginFunctions();
        this.functionBuilder.beginFunction(function);
        return this.functionBuilder;
    }
    
    public FunctionBuilder<T> beginFunctions()
    {
        if (this.functionBuilder == null)
        {
            this.functionBuilder = new MySQLFunctionBuilder(this, builder);
        }
        this.functionBuilder.query = new StringBuilder();
        return functionBuilder;
    }
    
    public T function(String function)
    {
        this.beginFunctions();
        this.functionBuilder.function(function);
        return (T)this.functionBuilder.endFunctions();
    }
    
    public T orderBy(String... cols)
    {
        Validate.notEmpty(cols, "No cols specified!");
        
        this.query.append(" ORDER BY ").append(this.prepareName(cols[0], false));
        for (int i = 1; i < cols.length; ++i)
        {
            this.query.append(',').append(this.prepareName(cols[i], false));
        }
        return (T)this;
    }

    public T limit(int n)
    {
        this.query.append(" LIMIT ").append(n);
        return (T)this;
    }

    public T offset(int n)
    {
        this.query.append(" OFFSET ").append(n);
        return (T)this;
    }

    
    
    
}