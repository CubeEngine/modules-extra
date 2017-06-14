/*
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
package org.cubeengine.module.authorization;

import org.cubeengine.libcube.service.filesystem.FileUtil;
import org.cubeengine.libcube.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class HashHelper
{

    public static final String DEFAULT_ALGORITHM = "SHA-512";

    public static String loadStaticSalt(Path from)
    {
        String salt;
        if (Files.exists(from))
        {
            try
            {
                salt = new String(Files.readAllBytes(from), US_ASCII);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Salt file existed, but could not load it!", e);
            }
        }
        else
        {
            try
            {
                salt = generateStaticSalt();
                Files.write(from, salt.getBytes(US_ASCII), StandardOpenOption.SYNC);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Failed to store the static salt!", e);
            }
        }
        FileUtil.hideFile(from);
        FileUtil.setReadOnly(from);

        return salt;
    }

    public static String generateStaticSalt()
    {
        return StringUtils.randomString(new SecureRandom(), 32);
    }

    public static String toDynamicSalt(UUID id)
    {
        return id.toString();
    }

    public static String saltString(String input, String staticSalt, String dynamicSalt)
    {
        return staticSalt + dynamicSalt + input;
    }

    public static byte[] hash(String input) throws NoSuchAlgorithmException
    {
        return hash(input, DEFAULT_ALGORITHM);
    }

    public static byte[] hash(String input, String algorithm) throws NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return digest.digest(input.getBytes(US_ASCII));
    }

    public static boolean checkAgainstHash(byte[] reference, String password) throws NoSuchAlgorithmException
    {
        return checkAgainstHash(reference, password, DEFAULT_ALGORITHM);
    }

    public static boolean checkAgainstHash(byte[] reference, String password, String algorthim) throws NoSuchAlgorithmException
    {
        return Arrays.equals(reference, hash(password, algorthim));
    }
}
