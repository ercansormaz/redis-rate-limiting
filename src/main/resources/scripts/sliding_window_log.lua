redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[2])
local count = redis.call('ZCOUNT', KEYS[1], ARGV[2] + 1, ARGV[1])
if tonumber(count) < tonumber(ARGV[3]) then
    redis.call('ZADD', KEYS[1], ARGV[1], ARGV[5])
    redis.call('PEXPIRE', KEYS[1], ARGV[4])
    return 1
end
return 0