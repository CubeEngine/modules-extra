/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cubeisland.engine.core.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.cubeisland.engine.core.Core;
import de.cubeisland.engine.core.CubeEngine;

import de.cubeisland.engine.core.bukkit.BukkitCore;
import de.cubeisland.engine.core.util.Cleanable;

import org.slf4j.Logger;

import static de.cubeisland.engine.core.CubeEngine.runsOnWindows;

/**
 * Manages all the configurations of the CubeEngine.
 */
public class FileManager implements Cleanable
{
    private Logger logger;
    private final File dataFolder;
    private final File languageDir;
    private final File logDir;
    private final File modulesDir;
    private final File tempDir;
    private ConcurrentMap<File, Resource> fileSources;

    public FileManager(Core core, File dataFolder) throws IOException
    {
        java.util.logging.Logger logger = ((BukkitCore)core).getLogger();
        assert dataFolder != null: "The CubeEngine plugin folder must not be null!";
        if (!dataFolder.exists())
        {
            if (!dataFolder.mkdirs())
            {
                throw new IOException("The CubeEngine plugin folder could not be created: " + dataFolder.getAbsolutePath());
            }
            dataFolder.setWritable(true, true);
        }
        else if (!dataFolder.isDirectory())
        {
            throw new IOException("The CubeEngine plugin folder was found, but it doesn't seem to be directory: " + dataFolder.getAbsolutePath());
        }
        if (!dataFolder.canWrite() && !dataFolder.setWritable(true, true))
        {
            throw new IOException("The CubeEngine plugin folder is not writable: " + dataFolder.getAbsolutePath());
        }
        this.dataFolder = dataFolder;

        final File linkSource = new File(System.getProperty("user.dir", "."), CubeEngine.class.getSimpleName());
        if (!isSymLink(linkSource) && !createSymLink(linkSource, this.dataFolder))
        {
            logger.info("Linking to the CubeEngine directory failed! This can be ignored.");
        }

        this.languageDir = new File(this.dataFolder, "language");
        if (!this.languageDir.isDirectory() && !this.languageDir.mkdirs())
        {
            throw new IOException("Failed to create the language folder: " + this.languageDir.getAbsolutePath());
        }
        if (!this.languageDir.canWrite() && !this.languageDir.setWritable(true, true))
        {
            throw new IOException("The language folder is not writable!");
        }

        this.logDir = new File(this.dataFolder, "log");
        if (!this.logDir.isDirectory() && !this.logDir.mkdirs())
        {
            throw new IOException("Failed to create the log folder: " + this.logDir.getAbsolutePath());
        }
        if (!this.logDir.canWrite() && !this.logDir.setWritable(true, true))
        {
            throw new IOException("The log folder is not writable!: " + this.logDir.getAbsolutePath());
        }

        this.modulesDir = new File(this.dataFolder, "modules");
        if (!this.modulesDir.isDirectory() && !this.modulesDir.mkdirs())
        {
            throw new IOException("Failed to create the modules folder: " + this.modulesDir.getAbsolutePath());
        }
        if (!this.modulesDir.canWrite() && !this.modulesDir.setWritable(true, true))
        {
            throw new IOException("The modules folder is not writable: " + this.modulesDir.getAbsolutePath());
        }

        this.tempDir = new File(this.dataFolder, "temp");
        if (!this.tempDir.isDirectory() && !this.tempDir.mkdirs())
        {
            throw new IOException("Failed to create the temp folder: " + this.tempDir.getAbsolutePath());
        }
        if (!this.tempDir.canWrite() && !this.tempDir.setWritable(true, true))
        {
            throw new IOException("The temp folder is not writable: " + this.tempDir.getAbsolutePath());
        }
        if (!hideFile(this.tempDir))
        {
            logger.info("Hiding the temp folder failed! This can be ignored!");
        }

        this.fileSources = new ConcurrentHashMap<File, Resource>();
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    public static boolean hideFile(File file)
    {
        if (runsOnWindows())
        {
            try
            {
                return Runtime.getRuntime().exec(new String[] {
                "attrib", "+H", file.getAbsolutePath()
                }).waitFor() == 0;
            }
            catch (Exception e)
            {}
        }
        return false;
    }

    public static boolean createSymLink(File source, File target)
    {
        final String[] command;
        if (runsOnWindows())
        {
            if (target.isDirectory())
            {
                command = new String[] {
                "cmd", "/c", "mklink", "/d", source.getAbsolutePath(), target.getAbsolutePath()
                };
            }
            else
            {
                command = new String[] {
                "cmd", "/c", "mklink", source.getAbsolutePath(), target.getAbsolutePath()
                };
            }
        }
        else
        {
            command = new String[] {
            "ln", "-s", target.getAbsolutePath(), source.getAbsolutePath()
            };
        }
        try
        {
            return Runtime.getRuntime().exec(command).waitFor() == 0;
        }
        catch (Exception e)
        {}
        return false;
    }

    public static boolean isSymLink(File file) throws IOException
    {
        final File canon;
        if (file.getParent() == null)
        {
            canon = file;
        }
        else
        {
            canon = new File(file.getParentFile().getCanonicalFile(), file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }

    /**
     * Returns the data folder of the CubeEngine
     *
     * @return a file
     */
    public File getDataFolder()
    {
        return this.dataFolder;
    }

    /**
     * Returns the language directory
     *
     * @return the directory
     */
    public File getLanguageDir()
    {
        return this.languageDir;
    }

    /**
     * Returns the log directory
     *
     * @return the directory
     */
    public File getLogDir()
    {
        return this.logDir;
    }

    /**
     * Returns the modules directory
     *
     * @return the directory
     */
    public File getModulesDir()
    {
        return this.modulesDir;
    }

    /**
     * Returns the modules directory
     *
     * @return the directory
     */
    public File getTempDir()
    {
        return this.tempDir;
    }

    public void clearTempDir()
    {
        logger.debug("Clearing the temporary folder '{}'...", this.tempDir.getAbsolutePath());
        File[] files = this.tempDir.listFiles();
        if (files == null)
        {
            logger.info("Failed to list the temp folder for '{}'", this.getTempDir().getAbsolutePath());
            return;
        }
        for (File file : files)
        {
            try
            {
                deleteRecursive(file);
            }
            catch (IOException e)
            {
                logger.info("Failed to remove the file '{}'", file.getAbsolutePath());
            }
        }
        logger.debug("Temporary folder cleared!");
    }

    public static void deleteRecursive(File file) throws IOException
    {
        if (file == null)
        {
            return;
        }
        if (file.isDirectory())
        {
            File[] files = file.listFiles();
            if (files == null)
            {
                if (!file.canRead() && !file.setReadable(true) && (files = file.listFiles()) == null)
                {
                    throw new IOException("Failed to list the folder '" + file.getAbsolutePath() + "' due to missing read permissions");
                }
                if (files == null)
                {
                    throw new IOException("Failed to list the folder '" + file.getAbsolutePath() + "'");
                }
            }
            for (File f : files)
            {
                try
                {
                    deleteRecursive(f);
                }
                catch (FileNotFoundException ignored)
                {}
            }
        }
        if (!file.exists())
        {
            return;
        }
        if (!file.delete())
        {
            if (!file.canWrite())
            {
                if (file.setWritable(true) && file.delete())
                {
                    return;
                }
                throw new IOException("Failed to delete the file '" + file.getAbsolutePath() + "' due to missing write permissions");
            }
            if (!file.renameTo(file))
            {
                throw new IOException("Failed to delete the file '" + file.getAbsolutePath() + "' due to a possible lock");
            }
            throw new IOException("Failed to delete the file '" + file.getAbsolutePath() + "'");
        }
    }

    public static boolean copyFile(File source, File target) throws FileNotFoundException
    {
        final InputStream is = new FileInputStream(source);
        final OutputStream os = new FileOutputStream(target);

        try
        {
            copyFile(is, os);
            return true;
        }
        catch (IOException ignored)
        {}
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                CubeEngine.getLog().warn("Failed to close a file stream!", e);
            }

            try
            {
                os.close();
            }
            catch (IOException e)
            {
                CubeEngine.getLog().warn("Failed to close a file stream!", e);
            }
        }
        return false;
    }

    public static void copyFile(InputStream is, OutputStream os) throws IOException
    {
        final byte[] buffer = new byte[1024 * 4];
        int bytesRead;

        while ((bytesRead = is.read(buffer, 0, buffer.length)) > 0)
        {
            os.write(buffer, 0, bytesRead);
        }
    }

    private static String getSaneSource(Resource resource)
    {
        String source = resource.getSource();
        // we only accept absolute paths!
        if (!source.startsWith("/"))
        {
            source = "/" + source;
        }
        return source;
    }

    /**
     * Returns a resource as a stream
     *
     * @param resource the resource
     * @return a stream to read from
     */
    public InputStream getResourceStream(Resource resource)
    {
        if (resource == null)
        {
            return null;
        }
        return resource.getClass().getResourceAsStream(getSaneSource(resource));
    }

    /**
     * Returns a resource as a file by first copying it to the file system
     *
     * @param resource the resource
     * @return a file
     */
    public File getResourceFile(Resource resource)
    {
        assert resource != null: "The resource must not be null!";

        File file = this.dropResource(resource.getClass(), getSaneSource(resource), resource.getTarget(), false);
        this.fileSources.put(file, resource);
        return file;
    }

    /**
     * Drops an array of resources (usually the values of an enum)
     *
     * @param resources the resources
     */
    public void dropResources(Resource[] resources)
    {
        assert resources != null: "The resources must not be null!";

        for (Resource resource : resources)
        {
            this.getResourceFile(resource);
        }
    }

    /**
     * Drops an resource
     *
     * @param clazz     the class of the resource
     * @param resPath   the resource path
     * @param filePath  the target file path
     * @param overwrite wheter to overwrite an existing file
     * @return a file
     */
    public File dropResource(Class clazz, String resPath, String filePath, boolean overwrite)
    {
        assert filePath != null: "The file path must not be null!";
        assert resPath != null: "The resource path must not be null!";

        if (filePath.startsWith("/"))
        {
            filePath = filePath.substring(1);
        }
        return this.dropResource(clazz, resPath, new File(this.dataFolder, filePath.replace('\\', File.separatorChar).replace('/', File.separatorChar)), overwrite);
    }

    /**
     * Drops an resource
     *
     * @param clazz     the class of the resource
     * @param resPath   the resource path
     * @param file      the target file
     * @param overwrite whether to overwrite an existing file
     * @return a file
     */
    public File dropResource(Class clazz, String resPath, File file, boolean overwrite)
    {
        assert clazz != null: "The class must not be null!";
        assert resPath != null: "The resource path must not be null!";
        assert file != null: "The file must not be null!";
        if (file.exists() && !file.isFile())
        {
            throw new IllegalArgumentException("The given file exists, but is no file!");
        }
        if (file.exists() && !overwrite)
        {
            return file;
        }
        InputStream reader = clazz.getResourceAsStream(resPath);
        if (reader != null)
        {
            OutputStream writer = null;
            try
            {
                file.getParentFile().mkdirs();
                writer = new FileOutputStream(file);
                final byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) > 0)
                {
                    writer.write(buffer, 0, bytesRead);
                }
                writer.flush();
            }
            catch (IOException e)
            {
                logger.error(e.getMessage(), e);
            }
            finally
            {
                try
                {
                    reader.close();
                }
                catch (IOException ignored)
                {}
                if (writer != null)
                {
                    try
                    {
                        writer.close();
                    }
                    catch (IOException ignored)
                    {}
                }
            }
        }
        else
        {
            throw new RuntimeException("Could not find the resource '" + resPath + "'!");
        }
        return file;
    }

    /**
     * Revers look up for resources by file
     *
     * @param file the file
     * @return stream of the resource
     */
    public InputStream getSourceOf(File file)
    {
        return this.getResourceStream(this.fileSources.get(file));
    }

    @Override
    public void clean()
    {
        this.clearTempDir();
    }
}