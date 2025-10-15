local key = KEYS[1]
local nowMillis = tonumber(ARGV[1])
local windowDurationMillis = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local requestId = ARGV[4]

-- Remove expired entries
redis.call('ZREMRANGEBYSCORE', key, 0, nowMillis - windowDurationMillis)

-- Count current requests
local count = redis.call('ZCOUNT', key, nowMillis - windowDurationMillis + 1, nowMillis)

if count < limit then
    redis.call('ZADD', key, nowMillis, requestId)
    redis.call('PEXPIRE', key, windowDurationMillis)
    return 1
end

return 0