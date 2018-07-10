package com.fr.swift.file.system.impl;

import com.fr.ftp.FTPUtils;
import com.fr.ftp.client.FineFTP;
import com.fr.general.ComparatorUtils;
import com.fr.io.utils.ResourceIOUtils;
import com.fr.stable.Filter;
import com.fr.swift.config.bean.FtpRepositoryConfigBean;
import com.fr.swift.file.exception.SwiftFileException;
import com.fr.swift.file.system.AbstractFileSystem;
import com.fr.swift.file.system.SwiftFileSystem;
import com.fr.swift.file.system.pool.BaseRemoteSystemPool;
import com.fr.swift.file.system.pool.RemotePoolCreator;
import com.fr.swift.log.SwiftLoggers;
import com.fr.swift.util.Strings;
import com.fr.third.org.apache.commons.pool2.ObjectPool;

import java.io.InputStream;
import java.net.URI;

/**
 * @author yee
 * @date 2018/6/26
 */
public class FtpFileSystemImpl extends AbstractFileSystem<FtpRepositoryConfigBean> {

    private ObjectPool<FineFTP> clientPool;
    private BaseRemoteSystemPool<FtpFileSystemImpl> systemPool;
    private URI rootURI;

    public FtpFileSystemImpl(FtpRepositoryConfigBean config, URI uri, ObjectPool<FineFTP> clientPool) {
        super(config, uri);
        this.systemPool = (BaseRemoteSystemPool<FtpFileSystemImpl>) RemotePoolCreator.creator().getPool(config);
        this.clientPool = clientPool;
        rootURI = URI.create(Strings.trimSeparator(config.getRootPath() + "/", "/"));
    }

    private FineFTP acquireClient() {
        try {
            return this.clientPool.borrowObject();
        } catch (Exception e) {
            throw new RuntimeException("No FineFTP available, Please check configuration or network state!");
        }
    }

    private void returnClient(FineFTP fineFTP) {
        try {
            this.clientPool.returnObject(fineFTP);
        } catch (Exception e) {
            SwiftLoggers.getLogger().error(e.getMessage());
        }

    }

    @Override
    protected SwiftFileSystem[] list() throws SwiftFileException {
        FineFTP ftp = acquireClient();
        try {
            String[] children = FTPUtils.list(ftp, resolve(rootURI, getResourceURI().getPath()).getPath(), new Filter<String>() {
                @Override
                public boolean accept(String s) {
                    return true;
                }
            });
            if (null != children) {
                SwiftFileSystem[] childFileSystem = new SwiftFileSystem[children.length];
                for (int i = 0; i < children.length; i++) {
                    childFileSystem[i] = systemPool.borrowObject(resolve(getResourceURI(), children[i]));
                }
                return childFileSystem;
            }
            return new SwiftFileSystem[0];
        } catch (Exception e) {
            throw new SwiftFileException(e);
        } finally {
            returnClient(ftp);
        }

    }

    @Override
    public void write(URI remote, InputStream inputStream) throws SwiftFileException {
        FineFTP ftp = acquireClient();
        try {
            FTPUtils.write(ftp, resolve(rootURI, remote.getPath()).getPath(), inputStream);
        } catch (Exception e) {
            throw new SwiftFileException(e);
        } finally {
            returnClient(ftp);
        }
    }

    @Override
    public SwiftFileSystem read(URI remote) throws SwiftFileException {
        SwiftFileSystem fileSystem;
        if (ComparatorUtils.equals(remote, getResourceURI())) {
            fileSystem = this;
        } else {
            fileSystem = systemPool.borrowObject(remote);
        }
        if (fileSystem.isExists()) {
            return fileSystem;
        }
        throw new SwiftFileException(String.format("File path '%s' not exists!", remote.getPath()));
    }

    @Override
    public SwiftFileSystem parent() {
        return systemPool.borrowObject(getParentURI());
    }

    @Override
    public boolean remove(URI remote) throws SwiftFileException {
        FineFTP ftp = acquireClient();
        try {
            return FTPUtils.delete(ftp, resolve(rootURI, remote.getPath()).getPath());
        } catch (Exception e) {
            throw new SwiftFileException(e);
        } finally {
            returnClient(ftp);
        }
    }

    @Override
    public boolean renameTo(URI src, URI dest) throws SwiftFileException {
        FineFTP ftp = acquireClient();
        try {
            return FTPUtils.rename(ftp, resolve(rootURI, src.getPath()).getPath(), resolve(rootURI, dest.getPath()).getPath());
        } catch (Exception e) {
            throw new SwiftFileException(e);
        } finally {
            returnClient(ftp);
        }
    }

    @Override
    public boolean copy(URI src, URI dest) {
        return false;
    }

    @Override
    public boolean isExists() {
        FineFTP ftp = acquireClient();
        try {
            return FTPUtils.exist(ftp, resolve(rootURI, getResourceURI().getPath()).getPath());
        } finally {
            returnClient(ftp);
        }
    }

    @Override
    public boolean isDirectory() {
        FineFTP ftp = acquireClient();
        try {
            return FTPUtils.isDirectory(ftp, resolve(rootURI, getResourceURI().getPath()).getPath());
        } finally {
            returnClient(ftp);
        }
    }

    @Override
    public InputStream toStream() throws SwiftFileException {
        FineFTP ftp = acquireClient();
        try {
            return FTPUtils.read(ftp, resolve(rootURI, getResourceURI().getPath()).getPath());
        } catch (Exception e) {
            throw new SwiftFileException(e);
        } finally {
            returnClient(ftp);
        }
    }

    @Override
    public String getResourceName() {
        return ResourceIOUtils.getName(getResourceURI().getPath());
    }

    @Override
    public void mkdirs() {
        FineFTP ftp = acquireClient();
        try {
            FTPUtils.createDirectory(ftp, resolve(rootURI, getResourceURI().getPath()).getPath());
        } catch (Exception e) {
            SwiftLoggers.getLogger().error(e);
        } finally {
            returnClient(ftp);
        }
    }

    @Override
    public void close() throws SwiftFileException {
        try {
            clientPool.clear();
        } catch (Exception e) {
            throw new SwiftFileException(e);
        }
    }

    private URI resolve(URI uri, String resolve) {
        String path = uri.getPath();
        if (path.endsWith("/")) {
            return uri.resolve(resolve);
        }
        return URI.create(path + "/").resolve(resolve);
    }
}
