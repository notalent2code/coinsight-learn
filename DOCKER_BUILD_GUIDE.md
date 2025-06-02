# 🚀 Optimized Docker Build Guide

This guide explains the optimized Docker build setup for the Coinsight microservices project.

## 📋 Overview

The optimized build system provides:
- **40-60s** first build (downloading dependencies)
- **2-10s** subsequent builds (with dependency caching)
- **Parallel builds** for faster development
- **BuildKit integration** for advanced caching
- **Security best practices** (non-root users, minimal images)

## 🛠️ Quick Start

### 1. Generate Optimized Dockerfiles
```bash
./generate-dockerfiles.sh
```

### 2. Enable BuildKit (Optional - already enabled in scripts)
```bash
source buildkit.config
```

### 3. Build All Services
```bash
./build-optimized.sh
```

### 4. Quick Development Rebuild
```bash
# Rebuild specific service
./quick-rebuild.sh budget-service

# Rebuild all services
./quick-rebuild.sh
```

## 📊 Build Performance

### Expected Build Times:
- **First build**: 40-60 seconds per service
- **Dependency changes**: 30-40 seconds per service  
- **Code-only changes**: 2-10 seconds per service
- **No changes**: ~1 second per service

### Optimization Features:
- ✅ Multi-stage builds
- ✅ BuildKit cache mounts
- ✅ Dependency layer caching
- ✅ Minimal runtime images
- ✅ Parallel builds
- ✅ Optimized .dockerignore

## 🔧 Scripts Overview

### `generate-dockerfiles.sh`
- Creates optimized Dockerfiles for all services
- Uses a consistent template with best practices
- Backs up existing Dockerfiles

### `build-optimized.sh`
- Builds all services with optimal caching
- Shows build times and image sizes
- Validates Dockerfiles before building
- Uses BuildKit features

### `quick-rebuild.sh`
- Fast rebuilds for development
- Rebuilds specific services
- Automatically restarts services
- Shows service logs

### `buildkit.config`
- BuildKit configuration settings
- Cache optimization
- Development environment setup

## 📁 File Structure

```
coinsight/
├── 📄 Dockerfile.optimized.template    # Template for all services
├── 🔧 generate-dockerfiles.sh          # Generate optimized Dockerfiles
├── 🚀 build-optimized.sh               # Optimized build script
├── ⚡ quick-rebuild.sh                 # Development rebuild script
├── ⚙️  buildkit.config                 # BuildKit configuration
├── 🚫 .dockerignore                    # Optimized build context
└── 📋 DOCKER_BUILD_GUIDE.md           # This guide
```

## 🎯 Best Practices

### Development Workflow:
1. Make code changes
2. Run `./quick-rebuild.sh [service-name]`
3. Test changes
4. Repeat

### Production Workflow:
1. Update dependencies in pom.xml (if needed)
2. Run `./generate-dockerfiles.sh` (if Dockerfile template changed)
3. Run `./build-optimized.sh`
4. Deploy with `docker-compose up -d`

### Dependency Updates:
1. Update `pom.xml` files
2. Run `./build-optimized.sh` (will download new dependencies)
3. Subsequent builds will use cached dependencies

## 🔍 Troubleshooting

### Slow Builds?
```bash
# Check BuildKit is enabled
echo $DOCKER_BUILDKIT

# Clear Docker cache if needed
docker builder prune -a

# Check Docker system info
docker system df
```

### Cache Issues?
```bash
# Force rebuild without cache
docker-compose build --no-cache [service-name]

# Clean up build cache
docker system prune -f
```

### Missing Dependencies?
```bash
# Ensure all pom.xml files are present
find . -name "pom.xml"

# Check Dockerfile exists
ls -la */Dockerfile
```

## 📈 Performance Monitoring

### Build Time Tracking:
- Scripts show individual build times
- Monitor first vs subsequent build times
- Track dependency download times

### Image Size Optimization:
- Scripts show final image sizes
- Use Alpine Linux base images
- Multi-stage builds reduce final image size

### Cache Efficiency:
- BuildKit cache mounts reduce rebuild times
- Layer caching improves incremental builds
- Dependency caching speeds up development

## 🔒 Security Features

- ✅ Non-root users in containers
- ✅ Minimal runtime images (Alpine)
- ✅ Health checks included
- ✅ Proper file permissions
- ✅ No sensitive data in images

## 🚀 Advanced Usage

### Custom Service Build:
```bash
# Build with custom cache settings
DOCKER_BUILDKIT=1 docker-compose build --build-arg BUILDKIT_CACHE_MOUNT_NS=custom-cache budget-service
```

### Parallel Development:
```bash
# Build multiple services simultaneously
./build-optimized.sh &
./quick-rebuild.sh auth-service &
wait
```

### Production Optimization:
```bash
# Build with specific memory limits
docker-compose build --memory=2g --parallel budget-service
```

## 📞 Support

If you encounter issues:
1. Check this guide first
2. Verify Docker and Docker Compose versions
3. Ensure BuildKit is supported
4. Review script output for errors
5. Check Docker daemon logs if needed

---

Happy building! 🎉
